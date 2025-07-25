package kr.co.victoryfairy.core.api.controller;

import io.dodn.springboot.core.enums.MemberEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.core.api.domain.MatchDomain;
import kr.co.victoryfairy.core.api.domain.MemberDomain;
import kr.co.victoryfairy.core.api.service.MatchService;
import kr.co.victoryfairy.core.api.service.MemberService;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.model.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Member", description = "회원")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MatchService matchService;

    @Operation(summary = "sns 별 인증 주소 불러오기")
    @GetMapping("/auth-path")
    public CustomResponse<MemberDomain.MemberOauthPathResponse> authPath(
            @RequestParam @Validated
            @Schema(description = "인증 로그인 타입", example = "KAKAO", implementation = MemberEnum.SnsType.class)
            MemberEnum.SnsType snsType,
            @RequestParam(required = false)
            @Schema(description = "redirect url")
            String redirectUrl
    ) {
        var response = memberService.getOauthPath(snsType, redirectUrl);
        return CustomResponse.ok(response);
    }

    @Operation(summary = "로그인")
    @GetMapping("/login")
    public CustomResponse<MemberDomain.MemberLoginResponse> login(@Validated MemberDomain.MemberLoginRequest request) {
        var response = memberService.login(request);
        return CustomResponse.ok(response);
    }

    @Operation(summary = "로그아웃")
    @GetMapping("/logout")
    public CustomResponse<MessageEnum> logout() {
        return CustomResponse.ok(MessageEnum.Auth.LOGOUT);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "관심 팀 등록")
    @PutMapping("/team")
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
    public CustomResponse<MemberDomain.MemberCheckNickDuplicateResponse> checkNickNmDuplicate(@RequestBody String nickNm) {
        return CustomResponse.ok(memberService.checkNickNmDuplicate(nickNm));
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "프로필 사진 수정")
    @PatchMapping("/profile")
    public CustomResponse<MessageEnum> updateMemberProfile(@RequestBody @Validated MemberDomain.MemberProfileUpdateRequest request) {
        memberService.updateMemberProfile(request);
        return CustomResponse.ok(MessageEnum.Common.REQUEST);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "닉네임 수정")
    @PatchMapping("/nick-name")
    public CustomResponse<MessageEnum> updateMemberNickNm(@RequestBody @Validated MemberDomain.MemberNickNmUpdateRequest request) {
        memberService.updateMemberNickNm(request);
        return CustomResponse.ok(MessageEnum.Common.REQUEST);
    }

    @Operation(summary = "관심 팀 경기")
    @GetMapping("/match-today")
    public CustomResponse<List<MatchDomain.InterestTeamMatchInfoResponse>> findInterestMatch() {
        var response = matchService.findByTeam();
        return CustomResponse.ok(response);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "직관 승률")
    @GetMapping("/win-rate")
    public CustomResponse<MemberDomain.MemberHomeWinRateResponse> findHomeWinRate() {
        var response = memberService.findHomeWinRate();
        return CustomResponse.ok(response);
    }

}
