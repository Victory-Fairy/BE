package kr.co.victoryfairy.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import kr.co.victoryfairy.diary.presentation.ViewingStatisticsController;
import kr.co.victoryfairy.diary.presentation.ViewingStatisticsDomain;
import kr.co.victoryfairy.game.presentation.GameCommonController;
import kr.co.victoryfairy.member.presentation.MyPageController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

class FeatureLocalityContractTest {
    @Test
    void keepsMyPageMethodsOnTheirExistingPaths() throws Exception {
        assertThat(MyPageController.class.getDeclaredMethod("findMemberInfo").getAnnotation(GetMapping.class).value())
                .containsExactly("/member");
        assertThat(MyPageController.class.getDeclaredMethod("deleteMember",
                kr.co.victoryfairy.member.presentation.MyPageDomain.DeleteAccountRequest.class)
                .getAnnotation(DeleteMapping.class).value()).containsExactly("/delete-account");
        assertThat(ViewingStatisticsController.class.getDeclaredMethod("findVictoryPower", String.class)
                .getAnnotation(GetMapping.class).value()).containsExactly("/victory-power");
        assertThat(ViewingStatisticsController.class.getDeclaredMethod("findReport", String.class)
                .getAnnotation(GetMapping.class).value()).containsExactly("/report");
    }

    @Test
    void keepsCommonTeamAndSeatPaths() throws Exception {
        assertThat(GameCommonController.class.getDeclaredMethod("findAll").getAnnotation(GetMapping.class).value())
                .containsExactly("/team");
        assertThat(GameCommonController.class.getDeclaredMethod("findSeat", Long.class, String.class)
                .getAnnotation(GetMapping.class).value()).containsExactly("/seat/{id}");
    }

    @Test
    void keepsViewingPayloadFieldNames() {
        assertThat(components(ViewingStatisticsDomain.VictoryPowerResponse.class)).containsExactly("level", "power");
        assertThat(components(ViewingStatisticsDomain.ReportResponse.class))
                .containsExactly("stadium", "home", "viewStatistics");
        assertThat(components(ViewingStatisticsDomain.ViewTypeDto.class))
                .containsExactly("winAvg", "win", "lose", "draw", "cancel");
        assertThat(components(ViewingStatisticsDomain.ViewStatisticsDto.class))
                .containsExactly("winTeam", "lossTeam", "stadium", "winningStreak", "homeWinAvg", "stadiumWinAvg");
    }

    private String[] components(Class<?> type) {
        return Arrays.stream(type.getRecordComponents()).map(component -> component.getName()).toArray(String[]::new);
    }
}
