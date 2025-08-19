package kr.co.victoryfairy.core.admin.service.impl;

import kr.co.victoryfairy.core.admin.domain.AuthDomain;
import kr.co.victoryfairy.core.admin.service.AuthService;
import kr.co.victoryfairy.storage.db.core.repository.AdminRepository;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.model.AuthModel;
import kr.co.victoryfairy.support.service.JwtService;
import kr.co.victoryfairy.support.utils.RequestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;

    public AuthServiceImpl(JwtService jwtService, PasswordEncoder passwordEncoder, AdminRepository adminRepository) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.adminRepository = adminRepository;
    }

    @Override
    public AuthDomain.LoginResponse login(AuthDomain.LoginRequest request) {

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

        var adminDto = AuthModel.AdminDto.builder()
                .id(admin.getId())
                .build();

        var accessTokenDto = jwtService.makeAccessToken(adminDto);

        admin.updateLastLogin(RequestUtils.getRemoteIp(), LocalDateTime.now());
        adminRepository.save(admin);
        return new AuthDomain.LoginResponse(accessTokenDto.getAccessToken(), accessTokenDto.getRefreshToken());
    }
}
