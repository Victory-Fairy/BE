package kr.co.victoryfairy.core.craw.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.craw.service.KboLiveGameCrawler.Records;
import kr.co.victoryfairy.core.craw.service.KboLiveGameCrawler.Snapshot;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.StadiumEntity;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchCustomRepository;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchRepository;
import org.junit.jupiter.api.Test;

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
        assertThat(LiveGameSyncService.shouldPoll(matchAt.minusMinutes(10), matchAt,
                MatchEnum.MatchStatus.READY, false)).isTrue();
        assertThat(LiveGameSyncService.shouldPoll(matchAt.minusMinutes(11), matchAt,
                MatchEnum.MatchStatus.READY, false)).isFalse();
        assertThat(LiveGameSyncService.shouldPoll(matchAt.plusHours(4), matchAt,
                MatchEnum.MatchStatus.PROGRESS, false)).isTrue();
        assertThat(LiveGameSyncService.shouldPoll(matchAt.plusHours(4), matchAt,
                MatchEnum.MatchStatus.END, false)).isTrue();
        assertThat(LiveGameSyncService.shouldPoll(matchAt.plusHours(4), matchAt,
                MatchEnum.MatchStatus.END, true)).isFalse();
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
        assertThat(new Records(List.of(Map.of("name", "player")), List.of(), List.of(), List.of()).hasData())
            .isTrue();
    }

    @Test
    void continuesWhenOneMatchCannotBeStored() {
        var customRepository = mock(GameMatchCustomRepository.class);
        var gameMatchRepository = mock(GameMatchRepository.class);
        var redisHandler = mock(RedisHandler.class);
        var crawService = mock(CrawService.class);
        var crawler = mock(KboLiveGameCrawler.class);
        var first = match("first");
        var second = match("second");
        var records = new Records(List.of(), List.of(), List.of(), List.of());

        when(customRepository.findByMatchAt(any(), eq(MatchEnum.LeagueType.KBO)))
            .thenReturn(List.of(first, second));
        when(crawler.crawl(any(), any())).thenReturn(List.of(
                new Snapshot("first", MatchEnum.MatchStatus.READY, "경기예정", "-", null, null, records),
                new Snapshot("second", MatchEnum.MatchStatus.READY, "경기예정", "-", null, null, records)));
        doThrow(new IllegalStateException("redis unavailable"))
            .when(redisHandler).pushHash(any(), eq("first"), any());

        new LiveGameSyncService(customRepository, gameMatchRepository, redisHandler, crawService, crawler).sync();

        verify(redisHandler).pushHash(any(), eq("second"), any());
    }

    private static GameMatchEntity match(String id) {
        var match = mock(GameMatchEntity.class);
        var team = mock(TeamEntity.class);
        var stadium = mock(StadiumEntity.class);
        when(match.getId()).thenReturn(id);
        when(match.getMatchAt()).thenReturn(LocalDateTime.now().minusHours(1));
        when(match.getStatus()).thenReturn(MatchEnum.MatchStatus.PROGRESS);
        when(match.getIsMatchInfoCraw()).thenReturn(false);
        when(match.getSeries()).thenReturn(MatchEnum.SeriesType.REGULAR);
        when(match.getAwayTeamEntity()).thenReturn(team);
        when(match.getHomeTeamEntity()).thenReturn(team);
        when(match.getStadiumEntity()).thenReturn(stadium);
        when(match.getLeague()).thenReturn(MatchEnum.LeagueType.KBO);
        return match;
    }

}
