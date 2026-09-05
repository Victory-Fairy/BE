package kr.co.victoryfairy.diary.presentation;

import io.swagger.v3.oas.annotations.media.Schema;

public interface ViewingStatisticsDomain {
    record VictoryPowerResponse(Short level, Short power) {}
    record ReportResponse(@Schema(description = "직관") ViewTypeDto stadium,
            @Schema(description = "집관") ViewTypeDto home,
            @Schema(description = "관람 통계") ViewStatisticsDto viewStatistics) {}
    record ViewTypeDto(@Schema(description = "승률") Short winAvg, @Schema(description = "승") Short win,
            @Schema(description = "패") Short lose, @Schema(description = "무") Short draw,
            @Schema(description = "취소") Short cancel) {}
    record ViewStatisticsDto(@Schema(description = "최다 승리 팀") String winTeam,
            @Schema(description = "최다 패배 팀") String lossTeam, @Schema(description = "최다 방문 경기장") String stadium,
            @Schema(description = "최다 연승 수") Short winningStreak, @Schema(description = "집관 승률") Short homeWinAvg,
            @Schema(description = "직관 승률") Short stadiumWinAvg) {}
}
