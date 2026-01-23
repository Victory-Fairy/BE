package kr.co.victoryfairy.core.api.domain;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.MemberEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface MyPageDomain {

    @Schema(name = "Member.MemberInfoForMyPage")
    record MemberInfoForMyPageResponse(@Schema(description = "유저 id") Long id,
            @Schema(description = "프로필 이미지") ImageDto profile, @Schema(description = "닉네임") String nickNm,
            @Schema(description = "sns 타입") MemberEnum.SnsType snsType, @Schema(description = "응원 팀") TeamDto team) {
    }

    record ImageDto(Long id, String path, String saveName, String ext) {
    }

    record TeamDto(Long id, String name, String sponsorNm) {
    }

    record VictoryPowerResponse(Short level, Short power) {
    }

    record ReportResponse(@Schema(description = "직관") ViewTypeDto stadium, @Schema(description = "집관") ViewTypeDto home,
            @Schema(description = "관람 통계") ViewStatisticsDto viewStatistics) {
    }

    record ViewTypeDto(@Schema(description = "승률") Short winAvg, @Schema(description = "승") Short win,
            @Schema(description = "패") Short lose, @Schema(description = "무") Short draw,
            @Schema(description = "취소") Short cancel) {
    }

    record ViewStatisticsDto(@Schema(description = "최다 승리 팀") String winTeam,
            @Schema(description = "최다 패배 팀") String lossTeam, @Schema(description = "최다 방문 경기장") String stadium,
            @Schema(description = "최다 연승 수") Short winningStreak, @Schema(description = "집관 승률") Short homeWinAvg,
            @Schema(description = "직관 승률") Short stadiumWinAvg) {
    }

    record DeleteAccountRequest(String reason) {
    }

    record VisitInfoDto(int count, LocalDateTime lastVisited) {
    }

    record TeamResultDto(int count, LocalDateTime lastPlayedAt) {
    }

}
