package kr.co.victoryfairy.support.model;

import io.dodn.springboot.core.enums.MemberEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

public interface AuthModel {

    @Builder
    @Getter
    @Schema(name = "Auth.MemberDto")
    class MemberDto {

        @Schema(description = "member id")
        private Long id;

        @Schema(description = "member info")
        private MemberInfoDto memberInfo;

    }

    @Builder
    @Getter
    @Schema(name = "Auth.MemberInfoDto")
    class MemberInfoDto {

        @Schema(description = "sns 타입", example = "KAKAO", implementation = MemberEnum.SnsType.class)
        private MemberEnum.SnsType snsType;

        @Schema(description = "닉네임 등록 여부")
        private Boolean isNickNmAdded;

        @Schema(description = "응원하는 팀 등록 여부")
        private Boolean isTeamAdded;

    }

    @Builder
    @Getter
    @Schema(name = "Auth.AdminDto")
    class AdminDto {

        @Schema(description = "admin id")
        private Long id;

    }

}
