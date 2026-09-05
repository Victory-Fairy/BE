package kr.co.victoryfairy.diary.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import kr.co.victoryfairy.game.domain.MatchEnum;
import org.junit.jupiter.api.Test;

class DiaryTest {

    @Test
    void update_preserves_identity_and_marks_the_new_values() {
        var diary = new Diary(7L, 3L, "match", 1L, "old", DiaryEnum.ViewType.HOME,
                DiaryEnum.WeatherType.SUNNY, DiaryEnum.MoodType.HAPPY, "old", false, LocalDateTime.MIN,
                LocalDateTime.MIN);

        var updated = diary.update(2L, "new", DiaryEnum.ViewType.STADIUM, DiaryEnum.MoodType.SAD,
                DiaryEnum.WeatherType.RAIN, "changed");

        assertThat(updated.id()).isEqualTo(7L);
        assertThat(updated.memberId()).isEqualTo(3L);
        assertThat(updated.gameMatchId()).isEqualTo("match");
        assertThat(updated.teamId()).isEqualTo(2L);
        assertThat(updated.teamName()).isEqualTo("new");
        assertThat(updated.viewType()).isEqualTo(DiaryEnum.ViewType.STADIUM);
        assertThat(updated.mood()).isEqualTo(DiaryEnum.MoodType.SAD);
        assertThat(updated.weather()).isEqualTo(DiaryEnum.WeatherType.RAIN);
        assertThat(updated.content()).isEqualTo("changed");
    }

    @Test
    void canceled_match_is_terminal_draw_without_scores() {
        assertThat(Diary.result(MatchEnum.MatchStatus.CANCELED, null)).isEqualTo(MatchEnum.ResultType.DRAW);
        assertThat(Diary.isRecordable(MatchEnum.MatchStatus.CANCELED, null, null)).isTrue();
    }

    @Test
    void ended_match_requires_both_scores_and_keeps_the_selected_team_result() {
        assertThat(Diary.isRecordable(MatchEnum.MatchStatus.END, (short) 2, null)).isFalse();
        assertThat(Diary.result(MatchEnum.MatchStatus.END, MatchEnum.ResultType.WIN))
            .isEqualTo(MatchEnum.ResultType.WIN);
        assertThat(Diary.result(MatchEnum.MatchStatus.END, MatchEnum.ResultType.LOSS))
            .isEqualTo(MatchEnum.ResultType.LOSS);
    }
}
