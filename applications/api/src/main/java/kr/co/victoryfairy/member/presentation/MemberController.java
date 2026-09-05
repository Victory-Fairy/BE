package kr.co.victoryfairy.member.presentation;

import kr.co.victoryfairy.member.domain.MemberEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.game.presentation.MatchDomain;
import kr.co.victoryfairy.member.presentation.MemberDomain;
import kr.co.victoryfairy.game.application.GameQueryService;
import kr.co.victoryfairy.member.application.MemberAuthService;
import kr.co.victoryfairy.member.application.MemberCommandService;
import kr.co.victoryfairy.member.application.MemberQueryService;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.response.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Member", description = "회원")
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberAuthService authService;

    private final MemberCommandService commandService;

    private final MemberQueryService queryService;

    private final GameQueryService matchService;

    @Operation(summary = "sns 별 인증 주소 불러오기")
    @GetMapping("/auth-path")
    public CustomResponse<MemberDomain.MemberOauthPathResponse> authPath(
            @RequestParam @Validated @Schema(description = "인증 로그인 타입", example = "KAKAO",
                    implementation = MemberEnum.SnsType.class) MemberEnum.SnsType snsType,
            @RequestParam(required = false) @Schema(description = "redirect url") String redirectUrl) {
        var response = authService.getOauthPath(snsType, redirectUrl);
        return CustomResponse.ok(response);
    }

    @Operation(summary = "로그인")
    @GetMapping("/login")
    public CustomResponse<MemberDomain.MemberLoginResponse> login(@Validated MemberDomain.MemberLoginRequest request) {
        var response = authService.login(request);
        return CustomResponse.ok(response);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public CustomResponse<MessageEnum> logout() {
        authService.logout();
        return CustomResponse.ok(MessageEnum.Auth.LOGOUT);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "관심 팀 등록")
    @PutMapping("/team")
    public CustomResponse<MessageEnum> updateTeam(
            @RequestBody @Validated MemberDomain.MemberTeamUpdateRequest request) {
        commandService.updateTeam(request);
        return CustomResponse.ok(MessageEnum.Common.UPDATE);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "닉네임 중복 체크")
    @PostMapping("/check-nick-duplicate")
    public CustomResponse<MemberDomain.MemberCheckNickDuplicateResponse> checkNickNmDuplicate(
            @RequestBody String nickNm) {
        return CustomResponse.ok(queryService.checkNickNmDuplicate(nickNm));
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "프로필 사진 수정")
    @PatchMapping("/profile")
    public CustomResponse<MessageEnum> updateMemberProfile(
            @RequestBody @Validated MemberDomain.MemberProfileUpdateRequest request) {
        commandService.updateMemberProfile(request);
        return CustomResponse.ok(MessageEnum.Common.REQUEST);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "닉네임 수정")
    @PatchMapping("/nick-name")
    public CustomResponse<MessageEnum> updateMemberNickNm(
            @RequestBody @Validated MemberDomain.MemberNickNmUpdateRequest request) {
        commandService.updateMemberNickNm(request);
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
        var response = queryService.findHomeWinRate();
        return CustomResponse.ok(response);
    }

    @Operation(summary = "토큰 재발행")
    @PatchMapping("/refresh-token")
    public CustomResponse<MemberDomain.RefreshTokenResponse> refreshToken(
            @RequestParam(required = true) String refreshToken) {
        var response = authService.refreshToken(refreshToken);
        return CustomResponse.ok(response);
    }

}
