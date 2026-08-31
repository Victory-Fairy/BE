package kr.co.victoryfairy.core.craw.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.common.service.GameRecordDomainService;
import kr.co.victoryfairy.core.craw.service.KboLiveGameCrawler.Records;
import kr.co.victoryfairy.core.craw.service.KboLiveGameCrawler.Snapshot;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.StadiumEntity;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchCustomRepository;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchRepository;
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
        var customRepository = mock(GameMatchCustomRepository.class);
        var date = matchAt.toLocalDate();
        var late = match("late", matchAt, MatchEnum.MatchStatus.READY, false);
        var early = match("early", matchAt.minusMinutes(90), MatchEnum.MatchStatus.READY, false);
        when(customRepository.findByMatchAt(date, MatchEnum.LeagueType.KBO)).thenReturn(List.of(late, early));

        var now = matchAt.minusHours(8);
        var next = service(customRepository).nextExecutionAt(date, now, now);

        assertThat(next).contains(LocalDateTime.of(2026, 8, 30, 16, 50));
    }

    @Test
    void schedulesAnActiveMatchAtTheNextPollingTime() {
        var customRepository = mock(GameMatchCustomRepository.class);
        var date = matchAt.toLocalDate();
        var active = match("active", matchAt, MatchEnum.MatchStatus.PROGRESS, false);
        when(customRepository.findByMatchAt(date, MatchEnum.LeagueType.KBO)).thenReturn(List.of(active));

        var nextPollingTime = LocalDateTime.of(2026, 8, 30, 18, 40);
        var next = service(customRepository).nextExecutionAt(date, matchAt.plusMinutes(1), nextPollingTime);

        assertThat(next).contains(nextPollingTime);
    }

    @Test
    void stopsSchedulingWhenEveryMatchIsComplete() {
        var customRepository = mock(GameMatchCustomRepository.class);
        var date = matchAt.toLocalDate();
        var ended = match("ended", matchAt, MatchEnum.MatchStatus.END, true);
        var canceled = match("canceled", matchAt, MatchEnum.MatchStatus.CANCELED, false);
        when(customRepository.findByMatchAt(date, MatchEnum.LeagueType.KBO)).thenReturn(List.of(ended, canceled));

        var now = matchAt.plusHours(4);

        assertThat(service(customRepository).nextExecutionAt(date, now, now)).isEmpty();
    }

    @Test
    void continuesWhenOneMatchCannotBeStored() {
        var customRepository = mock(GameMatchCustomRepository.class);
        var gameMatchRepository = mock(GameMatchRepository.class);
        var redisHandler = mock(RedisHandler.class);
        var gameCrawler = mock(KboGameCrawler.class);
        var liveCrawler = mock(KboLiveGameCrawler.class);
        var first = match("first");
        var second = match("second");
        var records = new Records(List.of(), List.of(), List.of(), List.of());

        when(customRepository.findByMatchAt(any(), eq(MatchEnum.LeagueType.KBO))).thenReturn(List.of(first, second));
        when(liveCrawler.crawl(any(), any()))
            .thenReturn(List.of(new Snapshot("first", MatchEnum.MatchStatus.READY, "경기예정", "-", null, null, records),
                    new Snapshot("second", MatchEnum.MatchStatus.READY, "경기예정", "-", null, null, records)));
        doThrow(new IllegalStateException("redis unavailable")).when(redisHandler).pushHash(any(), eq("first"), any());

        new LiveGameSyncService(customRepository, gameMatchRepository, redisHandler, gameCrawler, liveCrawler,
                mock(GameRecordDomainService.class))
            .sync();

        verify(redisHandler).pushHash(any(), eq("second"), any());
    }

    @Test
    void recoversUnratedDiariesWhenAMatchEnds() {
        var customRepository = mock(GameMatchCustomRepository.class);
        var gameMatchRepository = mock(GameMatchRepository.class);
        var redisHandler = mock(RedisHandler.class);
        var gameCrawler = mock(KboGameCrawler.class);
        var crawler = mock(KboLiveGameCrawler.class);
        var gameRecordService = mock(GameRecordDomainService.class);
        var away = new TeamEntity(1L, "KT", "KT");
        var home = new TeamEntity(2L, "LG", "LG");
        var match = GameMatchEntity.builder()
            .id("ended")
            .league(MatchEnum.LeagueType.KBO)
            .series(MatchEnum.SeriesType.REGULAR)
            .matchAt(LocalDateTime.now().minusHours(1))
            .awayTeamEntity(away)
            .homeTeamEntity(home)
            .stadiumEntity(StadiumEntity.builder().id(1L).shortName("잠실").build())
            .status(MatchEnum.MatchStatus.PROGRESS)
            .isMatchInfoCraw(false)
            .build();
        var records = new Records(List.of(), List.of(), List.of(), List.of());
        when(customRepository.findByMatchAt(any(), eq(MatchEnum.LeagueType.KBO))).thenReturn(List.of(match));
        when(crawler.crawl(any(), any())).thenReturn(
                List.of(new Snapshot("ended", MatchEnum.MatchStatus.END, "경기종료", "-", (short) 6, (short) 13, records)));
        when(gameMatchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new LiveGameSyncService(customRepository, gameMatchRepository, redisHandler, gameCrawler, crawler,
                gameRecordService)
            .sync();

        var savedMatch = ArgumentCaptor.forClass(GameMatchEntity.class);
        verify(gameRecordService).recover(savedMatch.capture());
        assertThat(savedMatch.getValue().getStatus()).isEqualTo(MatchEnum.MatchStatus.END);
    }

    private static GameMatchEntity match(String id) {
        return match(id, LocalDateTime.now().minusHours(1), MatchEnum.MatchStatus.PROGRESS, false);
    }

    private static GameMatchEntity match(String id, LocalDateTime matchAt, MatchEnum.MatchStatus status,
            boolean detailCrawled) {
        var match = mock(GameMatchEntity.class);
        var team = mock(TeamEntity.class);
        var stadium = mock(StadiumEntity.class);
        when(match.getId()).thenReturn(id);
        when(match.getMatchAt()).thenReturn(matchAt);
        when(match.getStatus()).thenReturn(status);
        when(match.getIsMatchInfoCraw()).thenReturn(detailCrawled);
        when(match.getSeries()).thenReturn(MatchEnum.SeriesType.REGULAR);
        when(match.getAwayTeamEntity()).thenReturn(team);
        when(match.getHomeTeamEntity()).thenReturn(team);
        when(match.getStadiumEntity()).thenReturn(stadium);
        when(match.getLeague()).thenReturn(MatchEnum.LeagueType.KBO);
        return match;
    }

    private static LiveGameSyncService service(GameMatchCustomRepository customRepository) {
        return new LiveGameSyncService(customRepository, mock(GameMatchRepository.class), mock(RedisHandler.class),
                mock(KboGameCrawler.class), mock(KboLiveGameCrawler.class), mock(GameRecordDomainService.class));
    }

}
