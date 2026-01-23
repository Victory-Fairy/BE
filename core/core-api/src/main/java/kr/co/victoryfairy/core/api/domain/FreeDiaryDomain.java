package kr.co.victoryfairy.core.api.domain;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.MatchEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface FreeDiaryDomain {

    @Schema(name = "FreeDiary.WriteRequest")
    record WriteRequest(
            @Schema(description = "경기 상태", implementation = MatchEnum.MatchStatus.class,
                    requiredMode = Schema.RequiredMode.REQUIRED) MatchEnum.MatchStatus matchStatus,

            @Schema(description = "홈팀 이름", requiredMode = Schema.RequiredMode.REQUIRED) String homeTeamName,

            @Schema(description = "어웨이팀 이름", requiredMode = Schema.RequiredMode.REQUIRED) String awayTeamName,

            @Schema(description = "홈팀 점수") Short homeScore,

            @Schema(description = "어웨이팀 점수") Short awayScore,

            @Schema(description = "경기장 이름") String stadiumName,

            @Schema(description = "경기 일시", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime matchAt,

            @Schema(description = "응원팀 이름") String teamName,

            @Schema(description = "관람 방식", implementation = DiaryEnum.ViewType.class) DiaryEnum.ViewType viewType,

            @Schema(description = "기분", implementation = DiaryEnum.MoodType.class) DiaryEnum.MoodType mood,

            @Schema(description = "날씨", implementation = DiaryEnum.WeatherType.class) DiaryEnum.WeatherType weather,

            @Schema(description = "내용") String content,

            @Schema(description = "좌석 후기") String seatReview,

            @Schema(description = "업로드 file id 리스트") List<Long> fileIdList,

            @Schema(description = "음식 리스트", example = "[\"맥주\", \"치킨\"]") List<String> foodNameList,

            @Schema(description = "함께한 사람 리스트") List<PartnerDto> partnerList) {
    }

    @Schema(name = "FreeDiary.WriteResponse")
    record WriteResponse(@Schema(description = "자유 일기 식별자") Long id) {
    }

    @Schema(name = "FreeDiary.UpdateRequest")
    record UpdateRequest(
            @Schema(description = "경기 상태", implementation = MatchEnum.MatchStatus.class,
                    requiredMode = Schema.RequiredMode.REQUIRED) MatchEnum.MatchStatus matchStatus,

            @Schema(description = "홈팀 이름", requiredMode = Schema.RequiredMode.REQUIRED) String homeTeamName,

            @Schema(description = "어웨이팀 이름", requiredMode = Schema.RequiredMode.REQUIRED) String awayTeamName,

            @Schema(description = "홈팀 점수") Short homeScore,

            @Schema(description = "어웨이팀 점수") Short awayScore,

            @Schema(description = "경기장 이름") String stadiumName,

            @Schema(description = "경기 일시", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime matchAt,

            @Schema(description = "응원팀 이름") String teamName,

            @Schema(description = "관람 방식", implementation = DiaryEnum.ViewType.class) DiaryEnum.ViewType viewType,

            @Schema(description = "기분", implementation = DiaryEnum.MoodType.class) DiaryEnum.MoodType mood,

            @Schema(description = "날씨", implementation = DiaryEnum.WeatherType.class) DiaryEnum.WeatherType weather,

            @Schema(description = "내용") String content,

            @Schema(description = "좌석 후기") String seatReview,

            @Schema(description = "업로드 file id 리스트") List<Long> fileIdList,

            @Schema(description = "음식 리스트", example = "[\"맥주\", \"치킨\"]") List<String> foodNameList,

            @Schema(description = "함께한 사람 리스트") List<PartnerDto> partnerList) {
    }

    @Schema(name = "FreeDiary.DetailResponse")
    record DetailResponse(@Schema(description = "자유 일기 ID") Long id,

            @Schema(description = "경기 상태") MatchEnum.MatchStatus matchStatus,

            @Schema(description = "홈팀 이름") String homeTeamName,

            @Schema(description = "어웨이팀 이름") String awayTeamName,

            @Schema(description = "홈팀 점수") Short homeScore,

            @Schema(description = "어웨이팀 점수") Short awayScore,

            @Schema(description = "경기장 이름") String stadiumName,

            @Schema(description = "경기 일시") LocalDateTime matchAt,

            @Schema(description = "응원팀 이름") String teamName,

            @Schema(description = "관람 방식") DiaryEnum.ViewType viewType,

            @Schema(description = "기분") DiaryEnum.MoodType mood,

            @Schema(description = "날씨") DiaryEnum.WeatherType weather,

            @Schema(description = "내용") String content,

            @Schema(description = "좌석 후기") String seatReview,

            @Schema(description = "이미지 리스트") List<ImageDto> images,

            @Schema(description = "음식 리스트") List<String> foodNameList,

            @Schema(description = "함께한 사람 리스트") List<PartnerDto> partnerList,

            @Schema(description = "등록 일자") LocalDateTime createdAt,

            @Schema(description = "수정 일자") LocalDateTime updatedAt) {
    }

    @Schema(name = "FreeDiary.ListResponse")
    record ListResponse(@Schema(description = "자유 일기 ID") Long id,

            @Schema(description = "경기 일자") LocalDate date,

            @Schema(description = "이미지") ImageDto image,

            @Schema(description = "이미지 리스트") List<ImageDto> images) {
    }

    @Schema(name = "FreeDiary.DailyListResponse")
    record DailyListResponse(@Schema(description = "자유 일기 ID") Long id,

            @Schema(description = "경기장 이름") String stadiumName,

            @Schema(description = "경기 일자") LocalDate date,

            @Schema(description = "경기 시간") String time,

            @Schema(description = "응원팀 이름") String teamName,

            @Schema(description = "홈팀 정보") TeamDto homeTeam,

            @Schema(description = "어웨이팀 정보") TeamDto awayTeam,

            @Schema(description = "내용") String content,

            @Schema(description = "경기 상태") MatchEnum.MatchStatus matchStatus,

            @Schema(description = "이미지") ImageDto image,

            @Schema(description = "등록 일자") LocalDateTime createdAt) {
    }

    record PartnerDto(@Schema(description = "함께한 사람 이름") String name,

            @Schema(description = "함께한 사람의 응원팀 ID") Long teamId) {
    }

    record TeamDto(@Schema(description = "팀 이름") String name,

            @Schema(description = "점수") Short score) {
    }

    record ImageDto(Long id, String path, String saveName, String ext) {
    }

}