package kr.co.victoryfairy.core.admin.domain;

import io.dodn.springboot.core.enums.MemberEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springdoc.core.annotations.ParameterObject;

public interface MemberDomain {

    @ParameterObject
    @Schema(name = "Member.MemberListRequest")
    record MemberListRequest(
            @Schema(description = "sns 종류", implementation = MemberEnum.SnsType.class, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            MemberEnum.SnsType snsType,
            @Schema(description = "검색", requiredMode =  Schema.RequiredMode.NOT_REQUIRED)
            String keyword,

            @Schema(description = "페이지 No", example = "1", requiredMode =  Schema.RequiredMode.REQUIRED)
            Integer page,
            @Schema(description = "페이지 크기", example = "10", requiredMode =  Schema.RequiredMode.REQUIRED)
            Integer size
    ) {}

    record MemberListResponse(
            Long id,
            String nickNm,
            MemberEnum.SnsType snsType,
            String email,
            Long teamId,
            String teamName,
            String sponsorNm
    ) {}
}
