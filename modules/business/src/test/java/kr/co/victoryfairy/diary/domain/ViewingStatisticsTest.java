package kr.co.victoryfairy.diary.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import kr.co.victoryfairy.game.domain.MatchEnum;
import org.junit.jupiter.api.Test;

class ViewingStatisticsTest {

    @Test
    void calculatesMixedReportWithRecentTieBreaksAndStreakReset() {
        var records = List.of(
                record(1, "LG", "잠실", DiaryEnum.ViewType.STADIUM, MatchEnum.MatchStatus.END, MatchEnum.ResultType.WIN, 1, 1),
                record(2, "KT", "수원", DiaryEnum.ViewType.STADIUM, MatchEnum.MatchStatus.END, MatchEnum.ResultType.LOSS, 2, 2),
                record(3, "LG", "잠실", DiaryEnum.ViewType.STADIUM, MatchEnum.MatchStatus.END, MatchEnum.ResultType.WIN, 3, 1),
                record(4, "KT", "수원", DiaryEnum.ViewType.STADIUM, MatchEnum.MatchStatus.CANCELED, MatchEnum.ResultType.DRAW, 4, 2),
                record(5, "두산", "고척", DiaryEnum.ViewType.HOME, MatchEnum.MatchStatus.END, MatchEnum.ResultType.LOSS, 5, 1),
                record(6, "두산", "고척", DiaryEnum.ViewType.HOME, MatchEnum.MatchStatus.END, MatchEnum.ResultType.WIN, 6, 1),
                record(7, "SSG", "문학", DiaryEnum.ViewType.HOME, MatchEnum.MatchStatus.END, MatchEnum.ResultType.DRAW, 7, 1));

        var result = ViewingStatistics.report(records);

        assertThat(result.stadium()).isEqualTo(new ViewingStatistics.ViewType((short) 67, (short) 2, (short) 1, (short) 0, (short) 1));
        assertThat(result.home()).isEqualTo(new ViewingStatistics.ViewType((short) 50, (short) 1, (short) 1, (short) 1, (short) 0));
        assertThat(result.statistics()).isEqualTo(new ViewingStatistics.Summary("LG", "두산", "수원", (short) 1, (short) 100, (short) 0));
    }

    @Test
    void roundsPowerAndMapsLevelBoundaries() {
        assertThat(ViewingStatistics.power(List.of(
                new ViewingRecordReader.Record(DiaryEnum.ViewType.STADIUM, MatchEnum.ResultType.WIN),
                new ViewingRecordReader.Record(DiaryEnum.ViewType.STADIUM, MatchEnum.ResultType.LOSS),
                new ViewingRecordReader.Record(DiaryEnum.ViewType.HOME, MatchEnum.ResultType.WIN)))).isEqualTo(new ViewingStatistics.Power((short) 75, (short) 4));
        assertThat(ViewingStatistics.level((short) 0)).isZero();
        assertThat(ViewingStatistics.level((short) 20)).isEqualTo((short) 2);
        assertThat(ViewingStatistics.level((short) 40)).isEqualTo((short) 3);
        assertThat(ViewingStatistics.level((short) 60)).isEqualTo((short) 4);
        assertThat(ViewingStatistics.level((short) 80)).isEqualTo((short) 5);
    }

    private GameRecord record(long id, String opponent, String stadium, DiaryEnum.ViewType viewType,
            MatchEnum.MatchStatus status, MatchEnum.ResultType result, int day, long homeTeamId) {
        return new GameRecord(id, 7L, id, "game-" + id, 1L, "한화", 2L, opponent, 3L, stadium, viewType,
                status, result, "2026", MatchEnum.LeagueType.KBO, LocalDateTime.of(2026, 8, day, 18, 0),
                homeTeamId, null, null);
    }
}
