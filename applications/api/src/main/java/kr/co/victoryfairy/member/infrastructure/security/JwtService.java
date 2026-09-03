package kr.co.victoryfairy.member.infrastructure.security;

import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.response.StatusEnum;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.member.infrastructure.security.AccessTokenDto;
import kr.co.victoryfairy.member.infrastructure.security.AuthModel;
import kr.co.victoryfairy.member.infrastructure.security.MemberAccount;
import kr.co.victoryfairy.member.infrastructure.security.JwtProperties;
import kr.co.victoryfairy.member.infrastructure.security.RefreshTokenRepository;
import kr.co.victoryfairy.member.infrastructure.security.AccessTokenCodec;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * JWT 토큰 생성 (Member용) - Access Token: jwtProperties.accessTokenExpireMinutes 사용 -
     * Refresh Token: jwtProperties.refreshTokenExpireDays 사용 + Redis 저장
     */
    public AccessTokenDto makeAccessToken(AuthModel.MemberDto member) {
        String ip = CurrentRequest.getRemoteIp();
        int accessTokenExpireMinutes = jwtProperties.getAccessTokenExpireMinutes();
        int refreshTokenExpireDays = jwtProperties.getRefreshTokenExpireDays();

        MemberAccount account = MemberAccount.builder()
            .id(member.getId())
            .expireMinutes(String.valueOf(accessTokenExpireMinutes))
            .ip(ip)
            .build();

        // Access Token, Refresh Token 생성
        AccessTokenCodec.makeAuthToken(account, jwtProperties, refreshTokenExpireDays);

        // Refresh Token을 Redis에 저장
        refreshTokenRepository.save(member.getId(), account.getRefreshToken(), refreshTokenExpireDays);
        log.info("토큰 발급 완료 - memberId: {}, accessTokenExpire: {}분, refreshTokenExpire: {}일", member.getId(),
                accessTokenExpireMinutes, refreshTokenExpireDays);

        return AccessTokenDto.builder()
            .accessToken(account.getAccessToken())
            .refreshToken(account.getRefreshToken())
            .build();
    }

    /**
     * JWT 토큰 생성 (Admin용)
     */
    public AccessTokenDto makeAccessToken(AuthModel.AdminDto admin) {
        String ip = CurrentRequest.getRemoteIp();
        int accessTokenExpireMinutes = jwtProperties.getAccessTokenExpireMinutes();
        int refreshTokenExpireDays = jwtProperties.getRefreshTokenExpireDays();

        MemberAccount account = MemberAccount.builder()
            .id(admin.getId())
            .expireMinutes(String.valueOf(accessTokenExpireMinutes))
            .ip(ip)
            .roles(List.of("ADMIN"))
            .build();

        // Access Token, Refresh Token 생성
        AccessTokenCodec.makeAuthToken(account, jwtProperties, refreshTokenExpireDays);

        // Refresh Token을 Redis에 저장 (Admin용 prefix 사용)
        refreshTokenRepository.saveAdmin(admin.getId(), account.getRefreshToken(), refreshTokenExpireDays);
        log.info("관리자 토큰 발급 완료 - adminId: {}", admin.getId());

        return AccessTokenDto.builder()
            .accessToken(account.getAccessToken())
            .refreshToken(account.getRefreshToken())
            .build();
    }

    /**
     * Refresh Token 검증 및 토큰 재발급 (Rotation 적용) - Redis에 저장된 Refresh Token과 비교 검증 - 검증 성공 시
     * 새로운 Access Token + Refresh Token 발급 - 기존 Refresh Token은 무효화 (Rotation)
     */
    public AccessTokenDto checkMemberRefreshToken(String refreshToken) {
        // JWT 자체 검증 (서명, 만료 등)
        MemberAccount memberAccount = AccessTokenCodec.parseRefreshToken(refreshToken, jwtProperties);

        // Redis에 저장된 Refresh Token과 비교
        boolean isValid = refreshTokenRepository.validate(memberAccount.getId(), refreshToken);
        if (!isValid) {
            log.warn("Refresh Token 불일치 또는 만료 - memberId: {}", memberAccount.getId());
            throw new CustomException(HttpStatus.UNAUTHORIZED, StatusEnum.STATUS_903);
        }

        // 새 토큰 발급 (Rotation)
        int accessTokenExpireMinutes = jwtProperties.getAccessTokenExpireMinutes();
        int refreshTokenExpireDays = jwtProperties.getRefreshTokenExpireDays();

        memberAccount.setExpireMinutes(String.valueOf(accessTokenExpireMinutes));
        AccessTokenCodec.makeAuthToken(memberAccount, jwtProperties, refreshTokenExpireDays);

        // 새 Refresh Token을 Redis에 저장 (기존 토큰 대체)
        refreshTokenRepository.rotate(memberAccount.getId(), memberAccount.getRefreshToken(), refreshTokenExpireDays);
        log.info("토큰 갱신 완료 (Rotation) - memberId: {}", memberAccount.getId());

        return AccessTokenDto.builder()
            .accessToken(memberAccount.getAccessToken())
            .refreshToken(memberAccount.getRefreshToken())
            .build();
    }

    /**
     * Admin Refresh Token 검증 및 토큰 재발급 (Rotation 적용)
     */
    public AccessTokenDto checkAdminRefreshToken(String refreshToken) {
        // JWT 자체 검증 (서명, 만료 등)
        MemberAccount adminAccount = AccessTokenCodec.parseRefreshToken(refreshToken, jwtProperties);

        // Redis에 저장된 Admin Refresh Token과 비교
        boolean isValid = refreshTokenRepository.validateAdmin(adminAccount.getId(), refreshToken);
        if (!isValid) {
            log.warn("Admin Refresh Token 불일치 또는 만료 - adminId: {}", adminAccount.getId());
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        // 새 토큰 발급 (Rotation)
        int accessTokenExpireMinutes = jwtProperties.getAccessTokenExpireMinutes();
        int refreshTokenExpireDays = jwtProperties.getRefreshTokenExpireDays();

        adminAccount.setExpireMinutes(String.valueOf(accessTokenExpireMinutes));
        adminAccount.setRoles(List.of("ADMIN"));
        AccessTokenCodec.makeAuthToken(adminAccount, jwtProperties, refreshTokenExpireDays);

        // 새 Refresh Token을 Redis에 저장 (기존 토큰 대체)
        refreshTokenRepository.rotateAdmin(adminAccount.getId(), adminAccount.getRefreshToken(),
                refreshTokenExpireDays);
        log.info("Admin 토큰 갱신 완료 (Rotation) - adminId: {}", adminAccount.getId());

        return AccessTokenDto.builder()
            .accessToken(adminAccount.getAccessToken())
            .refreshToken(adminAccount.getRefreshToken())
            .build();
    }

    /**
     * 로그아웃 - Redis에서 Refresh Token 삭제
     */
    public void logout(Long memberId) {
        refreshTokenRepository.delete(memberId);
        log.info("로그아웃 처리 완료 - memberId: {}", memberId);
    }

    /**
     * 강제 로그아웃 (관리자용)
     */
    public void forceLogout(Long memberId) {
        refreshTokenRepository.delete(memberId);
        log.info("강제 로그아웃 처리 완료 - memberId: {}", memberId);
    }

}
