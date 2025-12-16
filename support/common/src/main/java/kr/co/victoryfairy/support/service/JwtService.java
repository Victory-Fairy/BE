package kr.co.victoryfairy.support.service;

import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.model.AccessTokenDto;
import kr.co.victoryfairy.support.model.AuthModel;
import kr.co.victoryfairy.support.model.oauth.MemberAccount;
import kr.co.victoryfairy.support.properties.JwtProperties;
import kr.co.victoryfairy.support.repository.RefreshTokenRepository;
import kr.co.victoryfairy.support.utils.AccessTokenUtils;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * JWT 토큰 생성 (Member용)
     * - Access Token: jwtProperties.accessTokenExpireMinutes 사용
     * - Refresh Token: jwtProperties.refreshTokenExpireDays 사용 + Redis 저장
     */
    public AccessTokenDto makeAccessToken(AuthModel.MemberDto member) {
        String ip = RequestUtils.getRemoteIp();
        int accessTokenExpireMinutes = jwtProperties.getAccessTokenExpireMinutes();
        int refreshTokenExpireDays = jwtProperties.getRefreshTokenExpireDays();

        MemberAccount account = MemberAccount.builder()
                .id(member.getId())
                .expireMinutes(String.valueOf(accessTokenExpireMinutes))
                .ip(ip)
                .build();

        // Access Token, Refresh Token 생성
        AccessTokenUtils.makeAuthToken(account, jwtProperties, refreshTokenExpireDays);

        // Refresh Token을 Redis에 저장
        refreshTokenRepository.save(member.getId(), account.getRefreshToken(), refreshTokenExpireDays);
        log.info("토큰 발급 완료 - memberId: {}, accessTokenExpire: {}분, refreshTokenExpire: {}일",
                member.getId(), accessTokenExpireMinutes, refreshTokenExpireDays);

        return AccessTokenDto.builder()
                .accessToken(account.getAccessToken())
                .refreshToken(account.getRefreshToken())
                .build();
    }

    /**
     * JWT 토큰 생성 (Admin용)
     */
    public AccessTokenDto makeAccessToken(AuthModel.AdminDto admin) {
        String ip = RequestUtils.getRemoteIp();
        int accessTokenExpireMinutes = jwtProperties.getAccessTokenExpireMinutes();
        int refreshTokenExpireDays = jwtProperties.getRefreshTokenExpireDays();

        MemberAccount account = MemberAccount.builder()
                .id(admin.getId())
                .expireMinutes(String.valueOf(accessTokenExpireMinutes))
                .ip(ip)
                .build();

        // Access Token, Refresh Token 생성
        AccessTokenUtils.makeAuthToken(account, jwtProperties, refreshTokenExpireDays);

        // Refresh Token을 Redis에 저장 (Admin용 prefix 사용)
        refreshTokenRepository.saveAdmin(admin.getId(), account.getRefreshToken(), refreshTokenExpireDays);
        log.info("관리자 토큰 발급 완료 - adminId: {}", admin.getId());

        return AccessTokenDto.builder()
                .accessToken(account.getAccessToken())
                .refreshToken(account.getRefreshToken())
                .build();
    }

    /**
     * Refresh Token 검증 및 토큰 재발급 (Rotation 적용)
     * - Redis에 저장된 Refresh Token과 비교 검증
     * - 검증 성공 시 새로운 Access Token + Refresh Token 발급
     * - 기존 Refresh Token은 무효화 (Rotation)
     */
    public AccessTokenDto checkMemberRefreshToken(String refreshToken) {
        // JWT 자체 검증 (서명, 만료 등)
        MemberAccount memberAccount = AccessTokenUtils.parseRefreshToken(refreshToken, jwtProperties);

        // Redis에 저장된 Refresh Token과 비교
        boolean isValid = refreshTokenRepository.validate(memberAccount.getId(), refreshToken);
        if (!isValid) {
            log.warn("Refresh Token 불일치 또는 만료 - memberId: {}", memberAccount.getId());
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        // 새 토큰 발급 (Rotation)
        int accessTokenExpireMinutes = jwtProperties.getAccessTokenExpireMinutes();
        int refreshTokenExpireDays = jwtProperties.getRefreshTokenExpireDays();

        memberAccount.setExpireMinutes(String.valueOf(accessTokenExpireMinutes));
        AccessTokenUtils.makeAuthToken(memberAccount, jwtProperties, refreshTokenExpireDays);

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
        MemberAccount adminAccount = AccessTokenUtils.parseRefreshToken(refreshToken, jwtProperties);

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
        AccessTokenUtils.makeAuthToken(adminAccount, jwtProperties, refreshTokenExpireDays);

        // 새 Refresh Token을 Redis에 저장 (기존 토큰 대체)
        refreshTokenRepository.rotateAdmin(adminAccount.getId(), adminAccount.getRefreshToken(), refreshTokenExpireDays);
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
