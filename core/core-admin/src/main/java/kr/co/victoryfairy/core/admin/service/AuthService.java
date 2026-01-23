package kr.co.victoryfairy.core.admin.service;

import kr.co.victoryfairy.core.admin.domain.AuthDomain;

public interface AuthService {

    AuthDomain.LoginResponse login(AuthDomain.LoginRequest request);

    AuthDomain.RefreshTokenResponse refreshToken(String refreshToken);

}
