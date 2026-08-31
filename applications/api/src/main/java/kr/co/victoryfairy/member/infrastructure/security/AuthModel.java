package kr.co.victoryfairy.member.infrastructure.security;

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
