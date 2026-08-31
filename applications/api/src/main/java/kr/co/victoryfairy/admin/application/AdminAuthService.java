package kr.co.victoryfairy.admin.application;

import kr.co.victoryfairy.admin.presentation.AdminAuthDto;
import kr.co.victoryfairy.storage.db.core.repository.AdminRepository;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.member.infrastructure.security.AuthModel;
import kr.co.victoryfairy.member.infrastructure.security.JwtService;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AdminAuthService {

    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;

    private final AdminRepository adminRepository;

    public AdminAuthService(JwtService jwtService, PasswordEncoder passwordEncoder, AdminRepository adminRepository) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.adminRepository = adminRepository;
    }

    public AdminAuthDto.LoginResponse login(AdminAuthDto.LoginRequest request) {

        String adminId = request.id();
        String adminPwd = request.pwd();
        if (!StringUtils.hasText(adminId))
            throw new CustomException(HttpStatus.BAD_REQUEST, MessageEnum.Data.FAIL_NOT_NULL);

        if (!StringUtils.hasText(adminPwd))
            throw new CustomException(HttpStatus.BAD_REQUEST, MessageEnum.Data.FAIL_NOT_NULL);

        // 관리자 조회
        var admin = adminRepository.findByAdminId(adminId)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        if (!StringUtils.hasText(admin.getPwd()) || !passwordEncoder.matches(adminPwd, admin.getPwd()))
            throw new CustomException(HttpStatus.BAD_REQUEST, MessageEnum.Auth.FAIL_LOGIN);

        var adminDto = AuthModel.AdminDto.builder().id(admin.getId()).build();

        var accessTokenDto = jwtService.makeAccessToken(adminDto);

        admin.updateLastLogin(CurrentRequest.getRemoteIp(), LocalDateTime.now());
        adminRepository.save(admin);
        return new AdminAuthDto.LoginResponse(accessTokenDto.getAccessToken(), accessTokenDto.getRefreshToken());
    }

    public AdminAuthDto.RefreshTokenResponse refreshToken(String refreshToken) {
        var accessTokenDto = jwtService.checkAdminRefreshToken(refreshToken);
        return new AdminAuthDto.RefreshTokenResponse(accessTokenDto.getAccessToken(), accessTokenDto.getRefreshToken());
    }

}
