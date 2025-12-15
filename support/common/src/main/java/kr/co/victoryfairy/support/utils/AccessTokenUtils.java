package kr.co.victoryfairy.support.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.constant.StatusEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.model.oauth.MemberAccount;
import kr.co.victoryfairy.support.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class AccessTokenUtils {

    //private final JwtProperties jwtProperties;

    public static String getAccessToken(HttpServletRequest request) {
        var accessToken = request.getHeader("Authorization");
        log.info("accessToken : {}", accessToken);
        if (StringUtils.isNotEmpty(accessToken) && accessToken.startsWith("Bearer ")) {
            return accessToken.substring(7); // "Bearer " 접두사 제거
        }
        return null;
    }

    public static Boolean checkToken(HttpServletRequest request, JwtProperties jwtProperties) {
        var accessToken = getAccessToken(request);
        log.info("accessToken : {}", accessToken);
        // accessToken 유무 판단
        if (StringUtils.isEmpty(accessToken)) {
            return accessError(accessToken);
        }

        //유효시간내의 토큰 해석
        checkAccessToken(accessToken, jwtProperties, request);
        return true;
    }

    //토큰 체크
    public static Boolean checkAccessToken(String accessToken, JwtProperties jwtProperties, HttpServletRequest request) {

        var parseToken = JwtUtils.parseToken(accessToken, jwtProperties.getSecretKey());
        boolean isCertifiedToken = Boolean.parseBoolean(parseToken.get("isCertifiedToken").toString());
        if (!isCertifiedToken) {
            //accessToken is wrong
            return accessError(accessToken);
        }
        boolean isExpired = Boolean.parseBoolean(parseToken.get("isExpired").toString());
        if (isExpired) {
            // RefreshToken 요청
            return needCheckRefreshTokenError(accessToken);
        }

        // token 중에 저장된 유저정보 가져오기
        Object accountByToken = parseToken.get("accountByToken");
        ObjectMapper objectMapper = new ObjectMapper();

        var account = objectMapper.convertValue(accountByToken, MemberAccount.class);

        if (account == null) {
            return accessError(accessToken);
        } else {
            // URL이 /mgt로 시작하면 ADMIN 권한 확인
            String requestURI = request.getRequestURI();
            if (requestURI.contains("/mgt/")) {
                checkRoles(account.getRoles(), "ADMIN", accessToken);
            }
            request.setAttribute("accountByToken", account);
        }
        return true;
    }


    /**
     * Access Token, Refresh Token 생성
     * @param account 회원 계정 정보
     * @param jwtProperties JWT 설정
     * @param refreshTokenExpireDays Refresh Token 만료 일수
     */
    public static void makeAuthToken(MemberAccount account, JwtProperties jwtProperties, int refreshTokenExpireDays) {
        // token에 로그인 계정 정보 저장
        Map<String, Object> claims = new HashMap<>();

        account.setRefreshToken(null);
        account.setAccessToken(null);
        claims.put("accountByToken", account);

        // Access Token 생성 (분 단위)
        int accessTokenExpireMinutes = Integer.parseInt(account.getExpireMinutes());
        String token = JwtUtils.generateToken(claims, accessTokenExpireMinutes, jwtProperties.getSecretKey());
        account.setAccessToken(token);

        // Refresh Token 생성 (일 단위 → 분 단위로 변환)
        int refreshTokenExpireMinutes = refreshTokenExpireDays * 24 * 60;
        String refreshToken = JwtUtils.generateToken(claims, refreshTokenExpireMinutes, jwtProperties.getSecretKey());
        account.setRefreshToken(refreshToken);
    }

    /**
     * Refresh Token 파싱 (JWT 검증만, Redis 검증은 별도)
     * @param refreshToken Refresh Token
     * @param jwtProperties JWT 설정
     * @return MemberAccount 정보
     */
    public static MemberAccount parseRefreshToken(String refreshToken, JwtProperties jwtProperties) {
        var parseToken = JwtUtils.parseToken(refreshToken, jwtProperties.getSecretKey());

        boolean isCertifiedToken = Boolean.parseBoolean(parseToken.get("isCertifiedToken").toString());
        if (!isCertifiedToken) {
            log.warn("Refresh Token 서명 검증 실패: {}", refreshToken);
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        boolean isExpired = Boolean.parseBoolean(parseToken.get("isExpired").toString());
        if (isExpired) {
            log.warn("Refresh Token 만료됨: {}", refreshToken);
            throw new CustomException(MessageEnum.Auth.FAIL_VALID_EXPIRED);
        }

        // token에서 회원 정보 추출
        Object accountByToken = parseToken.get("accountByToken");
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(accountByToken, MemberAccount.class);
    }

    private static void checkRoles(List<String> roles, String targetRoles, String accessToken) {
        if (roles == null || !roles.contains(targetRoles)) {
            accessError(accessToken); // ADMIN 권한 없으면 에러 처리
        }
    }


    private static Boolean accessError(String accessToken) {
        log.warn("accessToken is wrong : {}", accessToken);
        throw new CustomException(HttpStatus.UNAUTHORIZED, StatusEnum.STATUS_901);
    }

    private static Boolean accessExpiredError(String accessToken) {
        log.warn("accessToken is expired : {}", accessToken);
        throw new CustomException(MessageEnum.Auth.FAIL_VALID_EXPIRED);
    }

    private static Boolean accessOverlapError(String accessToken) {
        log.warn("accessToken is overlapped : {}", accessToken);
        throw new CustomException(MessageEnum.Auth.FAIL_OVERLAP);
    }

    private static Boolean needCheckRefreshTokenError(String accessToken) {
        log.warn("accessToken is expired, need check refreshToken : {}", accessToken);
        throw new CustomException(HttpStatus.FORBIDDEN, StatusEnum.STATUS_902);
    }

}
