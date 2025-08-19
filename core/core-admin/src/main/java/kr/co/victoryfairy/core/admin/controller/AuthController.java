package kr.co.victoryfairy.core.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.core.admin.domain.AuthDomain;
import kr.co.victoryfairy.core.admin.service.AuthService;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.model.CustomResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public CustomResponse<AuthDomain.LoginResponse> login(@Validated @RequestBody AuthDomain.LoginRequest request) {
        var response = authService.login(request);
        return CustomResponse.ok(response);
    }

}
