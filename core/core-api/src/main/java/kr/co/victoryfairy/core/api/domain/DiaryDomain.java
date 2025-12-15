package kr.co.victoryfairy.core.api.domain;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.EventType;
import io.dodn.springboot.core.enums.MatchEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface DiaryDomain {

    @Schema(name = "Diary.WriteResponse")
    record WriteResponse(
            @Schema(description = "일기 식별자")
            Long id
    ) {}

    @Schema(name = "Diary.WriteRequest")
    record WriteRequest(
            @Schema(description = "응원팀 id", requiredMode = Schema.RequiredMode.REQUIRED)
            Long teamId,

            @Schema(description = "관람 방식", implementation = DiaryEnum.ViewType.class, requiredMode = Schema.RequiredMode.REQUIRED)
            DiaryEnum.ViewType viewType,                    // 관람 방식

            @Schema(description = "경기 식별자", example = "20240309HTNC0" , requiredMode = Schema.RequiredMode.REQUIRED)
            String gameMatchId,

            @Schema(description = "업로드 file id")
            List<Long> fileId,

            // 선택 입력값
            @Schema(description = "날씨", implementation = DiaryEnum.WeatherType.class)
            DiaryEnum.WeatherType weather,                     // 날씨

            @Schema(description = "기분", implementation = DiaryEnum.MoodType.class)
            DiaryEnum.MoodType mood,

            @Schema(description = "음식 리스트", example = "[\"맥주\", \"치킨\"]")
            List<String> foodNameList,          // 음식 리스트

            @Schema(description = "좌석 정보")
            SeatUseHistoryDto seat,             // 좌석

            @Schema(description = "내용")
            String content,

            @Schema(description = "함께한 사람 리스트")
            List<PartnerDto> partnerList       // 함께한 사람 리스트
    ) {}

    @Schema(name = "Diary.UpdateRequest")
    record UpdateRequest(
            @Schema(description = "응원팀 id", requiredMode = Schema.RequiredMode.REQUIRED)
            Long teamId,

            @Schema(description = "관람 방식", implementation = DiaryEnum.ViewType.class, requiredMode = Schema.RequiredMode.REQUIRED)
            DiaryEnum.ViewType viewType,                    // 관람 방식

            @Schema(description = "업로드 file id")
            List<Long> fileId,

            @Schema(description = "날씨", implementation = DiaryEnum.WeatherType.class)
            DiaryEnum.WeatherType weather,                     // 날씨

            @Schema(description = "기분", implementation = DiaryEnum.MoodType.class)
            DiaryEnum.MoodType mood,

            @Schema(description = "음식 리스트", example = "[\"맥주\", \"치킨\"]")
            List<String> foodNameList,          // 음식 리스트

            @Schema(description = "좌석 정보")
            SeatUseHistoryDto seat,             // 좌석

            @Schema(description = "내용")
            String content,

            @Schema(description = "함께한 사람 리스트")
            List<PartnerDto> partnerList       // 함께한 사람 리스트
    ) {}

    record PartnerDto(
            @Schema(description = "함께한 사람 이름", example = "홍길동")
            String name,

            @Schema(description = "함께한 사람의 응원 팀 id")
            Long teamId
    ) {}

    record SeatUseHistoryDto(
            @Schema(description = "좌석 식별자", example = "1")
            Long id,           // 좌석 식별자

            @Schema(description = "좌석 이름")
            String name
    ) {
    }

    record WriteEventDto(
            String gameId,
            Long memberId,
            Long diaryId,
            EventType type
    ) {}

    record ListResponse(
            Long id,
            Long teamId,
            LocalDate date,
            ImageDto image,
            List<ImageDto> images,
            MatchEnum.ResultType result
    ) {}

    record DailyListResponse(
            @Schema(description = "일기 id")
            Long id,
            @Schema(description = "경기장")
            String stadium,
            @Schema(description = "경기 일자")
            LocalDate date,
            @Schema(description = "경기 시간")
            String time,
            @Schema(description = "응원 팀 id")
            Long teamId,
            @Schema(description = "원정 팀")
            MatchDomain.TeamDto awayTeam,
            @Schema(description = "홈 팀")
            MatchDomain.TeamDto homeTeam,
            @Schema(description = "일기 내용")
            String content,
            @Schema(description = "경기 결과")
            MatchEnum.ResultType result,
            @Schema(description = "경기 상태")
            MatchEnum.MatchStatus status,
            @Schema(description = "이미지")
            ImageDto image,
            @Schema(description = "등록 일자")
            LocalDateTime createdAt
    ) {}

    record ImageDto(
            Long id,
            String path,
            String saveName,
            String ext
    ) {}

    record DiaryDetailResponse(
            @Schema(description = "응원팀 id", requiredMode = Schema.RequiredMode.REQUIRED)
            Long teamId,

            @Schema(description = "관람 방식", implementation = DiaryEnum.ViewType.class, requiredMode = Schema.RequiredMode.REQUIRED)
            DiaryEnum.ViewType viewType,

            @Schema(description = "경기 식별자", example = "20240309HTNC0" , requiredMode = Schema.RequiredMode.REQUIRED)
            String gameMatchId,

            List<ImageDto> file,

            @Schema(description = "날씨", implementation = DiaryEnum.WeatherType.class)
            DiaryEnum.WeatherType weather,                     // 날씨

            @Schema(description = "기분", implementation = DiaryEnum.MoodType.class)
            DiaryEnum.MoodType mood,

            @Schema(description = "음식 리스트", example = "[\"맥주\", \"치킨\"]")
            List<String> foodNameList,          // 음식 리스트

            @Schema(description = "좌석 정보")
            SeatUseHistoryDto seat,             // 좌석

            String content,

            @Schema(description = "함께한 사람 리스트")
            List<PartnerDto> partnerList,       // 함께한 사람 리스트

            @Schema(description = "경기 결과")
            MatchEnum.ResultType result,

            @Schema(description = "등록 일자")
            LocalDateTime createdAt,
            @Schema(description = "수정 일자")
            LocalDateTime updatedAt
    ) {}

}
