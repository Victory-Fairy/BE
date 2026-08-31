package kr.co.victoryfairy.core.api.admin.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.core.api.admin.application.AdminAuthService;
import kr.co.victoryfairy.support.model.CustomResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public CustomResponse<AdminAuthDto.LoginResponse> login(
            @Validated @RequestBody AdminAuthDto.LoginRequest request) {
        var response = adminAuthService.login(request);
        return CustomResponse.ok(response);
    }

    @Operation(summary = "토큰 재발행")
    @PatchMapping("/refresh-token")
    public CustomResponse<AdminAuthDto.RefreshTokenResponse> refreshToken(
            @RequestParam(required = true) String refreshToken) {
        var response = adminAuthService.refreshToken(refreshToken);
        return CustomResponse.ok(response);
    }

}
