package kr.co.victoryfairy.core.admin.domain;

import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.MemberEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springdoc.core.annotations.ParameterObject;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    record DiaryListResponse (
            Long id,
            Long teamId,
            String teamName,
            String content,

            Long memberId,
            String nickNm,

            LocalDateTime matchAt,
            MatchEnum.MatchStatus status
    ) {}
}
