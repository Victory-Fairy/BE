package kr.co.victoryfairy.member.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.member.presentation.MemberDomain;
import kr.co.victoryfairy.member.application.MemberAuthService;
import kr.co.victoryfairy.support.model.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberAuthService authService;

    @Operation(summary = "토큰 재발행")
    @PatchMapping("/refresh-token")
    public CustomResponse<MemberDomain.RefreshTokenResponse> refreshToken(@RequestParam(required = true) String refreshToken) {
        var response = authService.refreshToken(refreshToken);
        return CustomResponse.ok(response);
    }
}
