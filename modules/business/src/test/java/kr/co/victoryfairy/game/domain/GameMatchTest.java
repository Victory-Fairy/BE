package kr.co.victoryfairy.game.domain;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class GameMatchTest {

    @Test
    void updatesLiveStateAndCalculatesResultsWithoutLosingNullableState() {
        var match = match(null, null, MatchEnum.MatchStatus.READY, false);

        var live = match.updateLive(MatchEnum.MatchStatus.END, null, (short) 3, (short) 3);

        assertThat(live.result(false)).isEqualTo(MatchEnum.ResultType.DRAW);
        assertThat(live.result(true)).isEqualTo(MatchEnum.ResultType.DRAW);
        assertThat(live.reason()).isNull();
        assertThat(live.createdAt()).isEqualTo(match.createdAt());
        assertThat(match.result(false)).isNull();
        assertThat(live.markDetailCrawled().detailCrawled()).isTrue();
    }

    @Test
    void scheduleSyncKeepsPersistenceAndCrawlerFlags() {
        var existing = match((short) 1, (short) 0, MatchEnum.MatchStatus.END, true);
        var official = new GameMatch("same", MatchEnum.LeagueType.KBO, MatchEnum.MatchType.REGULAR,
                MatchEnum.SeriesType.REGULAR, "2026", LocalDateTime.of(2026, 4, 2, 18, 30), 3L, "A", null, 4L, "B",
                null, 9L, MatchEnum.MatchStatus.READY, null, false, false, true, null, null);

        var synced = existing.syncSchedule(official);

        assertThat(synced.awayTeamId()).isEqualTo(3L);
        assertThat(synced.detailCrawled()).isTrue();
        assertThat(synced.pushSent()).isTrue();
        assertThat(synced.createdAt()).isEqualTo(existing.createdAt());
    }

    private GameMatch match(Short away, Short home, MatchEnum.MatchStatus status, boolean crawled) {
        var created = LocalDateTime.of(2026, 1, 1, 0, 0);
        return new GameMatch("same", MatchEnum.LeagueType.KBO, MatchEnum.MatchType.REGULAR,
                MatchEnum.SeriesType.REGULAR, "2026", LocalDateTime.of(2026, 4, 1, 18, 30), 1L, "A", away, 2L, "B",
                home, null, status, null, crawled, true, true, created, null);
    }

}
