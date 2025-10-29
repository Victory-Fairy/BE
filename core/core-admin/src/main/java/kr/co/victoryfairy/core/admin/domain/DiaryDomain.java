package kr.co.victoryfairy.core.admin.domain;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.MemberEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springdoc.core.annotations.ParameterObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface DiaryDomain {

    @ParameterObject
    @Schema(name = "Diary.DiaryListRequest")
    record DiaryListRequest(
            @Schema(description = "경기 일자")
            LocalDate date,
            @Schema(description = "경기 상태", implementation = MatchEnum.MatchStatus.class, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            MatchEnum.MatchStatus status,

            @Schema(description = "페이지 No", example = "1", requiredMode =  Schema.RequiredMode.REQUIRED)
            Integer page,
            @Schema(description = "페이지 크기", example = "10", requiredMode =  Schema.RequiredMode.REQUIRED)
            Integer size
    ) {}

    @Schema(name = "Diary.DiaryListResponse")
    record DiaryListResponse (
            @Schema(description = "일기 id")
            Long id,
            @Schema(description = "응원 팀 명")
            String teamName,
            @Schema(description = "일기 내용")
            String content,

            @Schema(description = "닉네임")
            String nickNm,

            @Schema(description = "경기 일자")
            LocalDateTime matchAt,

            @Schema(description = "경기 상태")
            MatchEnum.MatchStatus status,
            @Schema(description = "기분")
            DiaryEnum.MoodType moodType,
            @Schema(description = "관람 방식, Stadium: 직관, HOME: 집관")
            DiaryEnum.ViewType viewType,
            @Schema(description = "날씨")
            DiaryEnum.WeatherType weatherType,
            @Schema(description = "음식")
            List<String> foods,
            @Schema(description = "함꼐한 사람")
            List<String> partners,
            @Schema(description = "좌석 정보")
            List<String> useHistories
    ) {}

}
