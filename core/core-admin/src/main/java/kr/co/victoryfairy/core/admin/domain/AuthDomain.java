package kr.co.victoryfairy.core.admin.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public interface AuthDomain {

    @Schema(name = "Auth.LoginRequest")
    record LoginRequest(
            @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
            String id,
            @Schema(description = "password", requiredMode = Schema.RequiredMode.REQUIRED)
            String pwd
    ) {}

    @Schema(name = "Auth.LoginResponse")
    record LoginResponse(
            String accessToken,
            String refreshToken
    ) {}

}
