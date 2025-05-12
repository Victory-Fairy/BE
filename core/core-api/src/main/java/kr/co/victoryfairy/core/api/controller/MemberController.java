package kr.co.victoryfairy.core.api.controller;

import io.dodn.springboot.core.enums.MemberEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.core.api.domain.MemberDomain;
import kr.co.victoryfairy.core.api.service.MemberService;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.model.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member", description = "회원")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "sns 별 인증 주소 불러오기")
    @GetMapping("/auth-path")
    public CustomResponse<MemberDomain.MemberOauthPathResponse> authPath(
            @RequestParam @Validated
            @Schema(description = "인증 로그인 타입", example = "KAKAO", implementation = MemberEnum.SnsType.class)
            MemberEnum.SnsType snsType
    ) {
        var response = memberService.getOauthPath(snsType);
        return CustomResponse.ok(response);
    }

    @Operation(summary = "로그인")
    @GetMapping("/login")
    public CustomResponse<MemberDomain.MemberLoginResponse> login(@Validated MemberDomain.MemberLoginRequest request) {
        var response = memberService.login(request);
        return CustomResponse.ok(response);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "관심 팀 등록")
    @PatchMapping("/team")
    public CustomResponse<MessageEnum> updateTeam(@RequestBody  @Validated MemberDomain.MemberTeamUpdateRequest request) {
        memberService.updateTeam(request);
        return CustomResponse.ok(MessageEnum.Common.UPDATE);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "선점한 닉네임 있는지 체크")
    @GetMapping("/check-nick")
    public CustomResponse<MemberDomain.MemberCheckNickNameResponse> checkNick() {
        var response = memberService.checkNick();
        return CustomResponse.ok(response);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "닉네임 중복 체크")
    @PostMapping("/check-nick-duplicate")
    public CustomResponse<MessageEnum> checkNickNmDuplicate(@RequestBody String nickNm) {
        return CustomResponse.ok(memberService.checkNickNmDuplicate(nickNm));
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "프로필 사진, 닉네임 수정")
    @PatchMapping("/info")
    public CustomResponse<MessageEnum> updateMemberInfo(@RequestBody @Validated MemberDomain.MemberInfoUpdateRequest request) {
        memberService.updateMemberInfo(request);
        return CustomResponse.ok(MessageEnum.Common.REQUEST);
    }
}
