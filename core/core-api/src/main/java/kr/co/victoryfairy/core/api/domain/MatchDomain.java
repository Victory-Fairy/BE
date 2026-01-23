package kr.co.victoryfairy.core.api.domain;

import io.dodn.springboot.core.enums.MatchEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface MatchDomain {

    record MatchListResponse(@Schema(description = "경기일자") LocalDate date, List<MatchListDto> matchList) {
    }

    record MatchListDto(String id, LocalDate date, String time, String stadium, MatchEnum.MatchStatus status,
            String statusDetail, TeamDto awayTeam, TeamDto homeTeam, Boolean isWrited) {
    }

    record TeamDto(Long id, String name, Short score, MatchEnum.ResultType result) {
    }

    record InterestTeamMatchInfoResponse(String id, LocalDate date, String time, StadiumDto stadium,
            MatchEnum.MatchStatus status, String statusDetail, TeamDto awayTeam, TeamDto homeTeam, Boolean isWrited) {
    }

    record MatchInfoResponse(String id, LocalDate date, String time, StadiumDto stadium, MatchEnum.MatchStatus status,
            String statusDetail, TeamDto awayTeam, TeamDto homeTeam) {
    }

    record StadiumDto(Long id, String shortName, String fullName) {
    }

    record RecordResponse(LocalDateTime date, TeamRecordDto awayTeam, TeamRecordDto homeTeam) {
    }

    record TeamRecordDto(String name, List<PitcherRecordDto> pitcherRecords, List<BatterRecordDto> batterRecords) {
    }

    record PitcherRecordDto(@Schema(description = "이름") String name, @Schema(description = "포지션") String position,
            @Schema(description = "이닝") String inning, @Schema(description = "투구수") Short pitchCount,
            @Schema(description = "4사구") Short fourBall, @Schema(description = "삼진") Short strikeOut,
            @Schema(description = "피안타") Short hit, @Schema(description = "피홈런") Short homeRun,
            @Schema(description = "실점") Short point) {
    }

    record PitcherRecordData(Short strikeOut, Short score, Short hit, String inning, String name, Short pitching,
            Short turn, String position, Short ballFour, Short homeRun) {
    }

    record BatterRecordData(Short strikeOut, Short score, Short hit, Short hitCount, Short hitScore, String name,
            Short turn, String position, Short ballFour, Short homeRun) {
    }

    record BatterRecordDto(@Schema(description = "이름") String name, @Schema(description = "포지션") String position,
            @Schema(description = "타순") Short battingOrder, @Schema(description = "타수") Short batCount,
            @Schema(description = "4사구") Short fourBall, @Schema(description = "삼진") Short strikeOut,
            @Schema(description = "득점") Short score, @Schema(description = "안타") Short hit,
            @Schema(description = "홈런") Short homeRun, @Schema(description = "타점") Short point) {
    }

    record TodayMatchListResponse(List<MatchListDto> matchList) {
    }

}
