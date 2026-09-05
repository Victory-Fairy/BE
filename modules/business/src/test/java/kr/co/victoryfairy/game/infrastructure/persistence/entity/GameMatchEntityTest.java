package kr.co.victoryfairy.game.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import kr.co.victoryfairy.game.domain.MatchEnum;
import org.junit.jupiter.api.Test;

class GameMatchEntityTest {

    @Test
    void syncScheduleUpdatesResultWithoutChangingRecoveryFlags() {
        GameMatchEntity existing = GameMatchEntity.builder()
            .id("20260819KTLG0")
            .matchAt(LocalDateTime.of(2026, 8, 19, 18, 30))
            .status(MatchEnum.MatchStatus.READY)
            .isMatchInfoCraw(true)
            .isSendPush(true)
            .build();
        GameMatchEntity official = GameMatchEntity.builder()
            .id("20260819KTLG0")
            .matchAt(LocalDateTime.of(2026, 8, 19, 19, 0))
            .awayScore((short) 0)
            .homeScore((short) 1)
            .status(MatchEnum.MatchStatus.END)
            .reason("-")
            .build();

        existing.syncSchedule(official);

        assertThat(existing.getId()).isEqualTo("20260819KTLG0");
        assertThat(existing.getMatchAt()).isEqualTo(LocalDateTime.of(2026, 8, 19, 19, 0));
        assertThat(existing.getAwayScore()).isZero();
        assertThat(existing.getHomeScore()).isEqualTo((short) 1);
        assertThat(existing.getStatus()).isEqualTo(MatchEnum.MatchStatus.END);
        assertThat(existing.getIsMatchInfoCraw()).isTrue();
        assertThat(existing.getIsSendPush()).isTrue();
    }

}
