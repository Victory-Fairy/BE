package kr.co.victoryfairy.game.crawler.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.diary.application.GameRecordDomainService;
import kr.co.victoryfairy.game.crawler.service.KboLiveGameCrawler.Records;
import kr.co.victoryfairy.game.crawler.service.KboLiveGameCrawler.Snapshot;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.game.domain.GameMatch;
import kr.co.victoryfairy.game.domain.GameMatchRepository;
import kr.co.victoryfairy.game.domain.StadiumReader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiveGameSyncServiceTest {

    private final LocalDateTime matchAt = LocalDateTime.of(2026, 8, 30, 18, 30);

    @Test
    void pollsFromTenMinutesBeforeUntilFinalRecordsAreSaved() {
        assertThat(
                LiveGameSyncService.shouldPoll(matchAt.minusMinutes(10), matchAt, MatchEnum.MatchStatus.READY, false))
            .isTrue();
        assertThat(
                LiveGameSyncService.shouldPoll(matchAt.minusMinutes(11), matchAt, MatchEnum.MatchStatus.READY, false))
            .isFalse();
        assertThat(LiveGameSyncService.shouldPoll(matchAt.plusHours(4), matchAt, MatchEnum.MatchStatus.PROGRESS, false))
            .isTrue();
        assertThat(LiveGameSyncService.shouldPoll(matchAt.plusHours(4), matchAt, MatchEnum.MatchStatus.END, false))
            .isTrue();
        assertThat(LiveGameSyncService.shouldPoll(matchAt.plusHours(4), matchAt, MatchEnum.MatchStatus.END, true))
            .isFalse();
    }

    @Test
    void mapsKboCssStatusToMatchStatus() {
        assertThat(KboLiveGameCrawler.toStatus("ing")).isEqualTo(MatchEnum.MatchStatus.PROGRESS);
        assertThat(KboLiveGameCrawler.toStatus("end")).isEqualTo(MatchEnum.MatchStatus.END);
        assertThat(KboLiveGameCrawler.toStatus("cancel")).isEqualTo(MatchEnum.MatchStatus.CANCELED);
        assertThat(KboLiveGameCrawler.toStatus("")).isEqualTo(MatchEnum.MatchStatus.READY);
    }

    @Test
    void distinguishesMissingRecordsFromCrawledRecords() {
        assertThat(Records.empty().hasData()).isFalse();
        assertThat(new Records(List.of(Map.of("name", "player")), List.of(), List.of(), List.of()).hasData()).isTrue();
    }

    @Test
    void schedulesFirstRunTenMinutesBeforeEarliestMatch() {
        var customRepository = mock(GameMatchRepository.class);
        var date = matchAt.toLocalDate();
        var late = match("late", matchAt, MatchEnum.MatchStatus.READY, false);
        var early = match("early", matchAt.minusMinutes(90), MatchEnum.MatchStatus.READY, false);
        when(customRepository.findByDate(date, MatchEnum.LeagueType.KBO)).thenReturn(List.of(late, early));

        var now = matchAt.minusHours(8);
        var next = service(customRepository).nextExecutionAt(date, now, now);

        assertThat(next).contains(LocalDateTime.of(2026, 8, 30, 16, 50));
    }

    @Test
    void schedulesAnActiveMatchAtTheNextPollingTime() {
        var customRepository = mock(GameMatchRepository.class);
        var date = matchAt.toLocalDate();
        var active = match("active", matchAt, MatchEnum.MatchStatus.PROGRESS, false);
        when(customRepository.findByDate(date, MatchEnum.LeagueType.KBO)).thenReturn(List.of(active));

        var nextPollingTime = LocalDateTime.of(2026, 8, 30, 18, 40);
        var next = service(customRepository).nextExecutionAt(date, matchAt.plusMinutes(1), nextPollingTime);

        assertThat(next).contains(nextPollingTime);
    }

    @Test
    void stopsSchedulingWhenEveryMatchIsComplete() {
        var customRepository = mock(GameMatchRepository.class);
        var date = matchAt.toLocalDate();
        var ended = match("ended", matchAt, MatchEnum.MatchStatus.END, true);
        var canceled = match("canceled", matchAt, MatchEnum.MatchStatus.CANCELED, false);
        when(customRepository.findByDate(date, MatchEnum.LeagueType.KBO)).thenReturn(List.of(ended, canceled));

        var now = matchAt.plusHours(4);

        assertThat(service(customRepository).nextExecutionAt(date, now, now)).isEmpty();
    }

    @Test
    void continuesWhenOneMatchCannotBeStored() {
        var customRepository = mock(GameMatchRepository.class);
        var gameMatchRepository = mock(GameMatchRepository.class);
        var redisHandler = mock(RedisHandler.class);
        var gameCrawler = mock(KboGameCrawler.class);
        var liveCrawler = mock(KboLiveGameCrawler.class);
        var first = match("first");
        var second = match("second");
        var records = new Records(List.of(), List.of(), List.of(), List.of());

        when(customRepository.findByDate(any(), eq(MatchEnum.LeagueType.KBO))).thenReturn(List.of(first, second));
        when(liveCrawler.crawl(any(), any()))
            .thenReturn(List.of(new Snapshot("first", MatchEnum.MatchStatus.READY, "경기예정", "-", null, null, records),
                    new Snapshot("second", MatchEnum.MatchStatus.READY, "경기예정", "-", null, null, records)));
        doThrow(new IllegalStateException("redis unavailable")).when(redisHandler).pushHash(any(), eq("first"), any());

        new LiveGameSyncService(customRepository, redisHandler, gameCrawler, liveCrawler,
                mock(GameRecordDomainService.class), mock(StadiumReader.class))
            .sync();

        verify(redisHandler).pushHash(any(), eq("second"), any());
    }

    @Test
    void recoversUnratedDiariesWhenAMatchEnds() {
        var customRepository = mock(GameMatchRepository.class);
        var gameMatchRepository = mock(GameMatchRepository.class);
        var redisHandler = mock(RedisHandler.class);
        var gameCrawler = mock(KboGameCrawler.class);
        var crawler = mock(KboLiveGameCrawler.class);
        var gameRecordService = mock(GameRecordDomainService.class);
        var match = match("ended", LocalDateTime.now().minusHours(1), MatchEnum.MatchStatus.PROGRESS, false);
        var records = new Records(List.of(), List.of(), List.of(), List.of());
        when(customRepository.findByDate(any(), eq(MatchEnum.LeagueType.KBO))).thenReturn(List.of(match));
        when(crawler.crawl(any(), any())).thenReturn(
                List.of(new Snapshot("ended", MatchEnum.MatchStatus.END, "경기종료", "-", (short) 6, (short) 13, records)));
        when(customRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new LiveGameSyncService(customRepository, redisHandler, gameCrawler, crawler, gameRecordService,
                mock(StadiumReader.class))
            .sync();

        var savedMatch = ArgumentCaptor.forClass(GameMatch.class);
        verify(gameRecordService).recover("ended");
        verify(customRepository).save(savedMatch.capture());
        assertThat(savedMatch.getValue().getStatus()).isEqualTo(MatchEnum.MatchStatus.END);
    }

    private static GameMatch match(String id) {
        return match(id, LocalDateTime.now().minusHours(1), MatchEnum.MatchStatus.PROGRESS, false);
    }

    private static GameMatch match(String id, LocalDateTime matchAt, MatchEnum.MatchStatus status,
            boolean detailCrawled) {
        return new GameMatch(id, MatchEnum.LeagueType.KBO, null, MatchEnum.SeriesType.REGULAR, "2026", matchAt, 1L,
                "KT", null, 2L, "LG", null, 1L, status, null, detailCrawled, false, true, null, null);
    }

    private static LiveGameSyncService service(GameMatchRepository repository) {
        return new LiveGameSyncService(repository, mock(RedisHandler.class), mock(KboGameCrawler.class),
                mock(KboLiveGameCrawler.class), mock(GameRecordDomainService.class), mock(StadiumReader.class));
    }

}
