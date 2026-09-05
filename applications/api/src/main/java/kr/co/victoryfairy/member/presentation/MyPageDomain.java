package kr.co.victoryfairy.member.presentation;

import kr.co.victoryfairy.member.domain.MemberEnum;
import io.swagger.v3.oas.annotations.media.Schema;


public interface MyPageDomain {

    @Schema(name = "Member.MemberInfoForMyPage")
    record MemberInfoForMyPageResponse(@Schema(description = "유저 id") Long id,
            @Schema(description = "프로필 이미지") ImageDto profile, @Schema(description = "닉네임") String nickNm,
            @Schema(description = "sns 타입") MemberEnum.SnsType snsType, @Schema(description = "응원 팀") TeamDto team) {
    }

    record ImageDto(Long id, String path, String saveName, String ext, String url) {
    }

    record TeamDto(Long id, String name, String sponsorNm) {
    }

    record DeleteAccountRequest(String reason) {
    }

}
