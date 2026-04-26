package kr.co.victoryfairy.core.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.core.api.domain.MemberDomain;
import kr.co.victoryfairy.core.api.service.MemberService;
import kr.co.victoryfairy.support.model.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @Operation(summary = "토큰 재발행")
    @PatchMapping("/refresh-token")
    public CustomResponse<MemberDomain.RefreshTokenResponse> refreshToken(@RequestParam(required = true) String refreshToken) {
        var response = memberService.refreshToken(refreshToken);
        return CustomResponse.ok(response);
    }
}
