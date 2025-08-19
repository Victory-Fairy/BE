package kr.co.victoryfairy.support.service;

import kr.co.victoryfairy.support.model.AccessTokenDto;
import kr.co.victoryfairy.support.model.AuthModel;
import kr.co.victoryfairy.support.model.oauth.MemberAccount;
import kr.co.victoryfairy.support.properties.JwtProperties;
import kr.co.victoryfairy.support.utils.AccessTokenUtils;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private final String ACCESS_TOKEN_TIME = "60";
    private final String ACCESS_TOKEN_TIME_DEV = Integer.toString(60 * 24 * 365);

    /**
     * jwt 토큰 생성(개발 단계에서는 토큰 시간을 길게 세팅)
     * @param member
     * @return
     */
    public AccessTokenDto makeAccessToken(AuthModel.MemberDto member) {
        //사용자 IP
        String ip = RequestUtils.getRemoteIp();

        //유효시간:분
        String expireMinutes = ACCESS_TOKEN_TIME_DEV;
        // 로컬이나 개발환경에서는 세션유지 오래 되게 조건 추가

        AccessTokenDto auth = new AccessTokenDto();

        MemberAccount account = MemberAccount.builder()
                .id(member.getId())
                .expireMinutes(expireMinutes)
                .ip(ip)
                .build();

        AccessTokenUtils.makeAuthToken(account, jwtProperties);

        auth.setAccessToken(account.getAccessToken());
        auth.setRefreshToken(account.getRefreshToken());

        return auth;
    }

    public AccessTokenDto makeAccessToken(AuthModel.AdminDto admin) {
        //사용자 IP
        String ip = RequestUtils.getRemoteIp();

        //유효시간:분
        String expireMinutes = ACCESS_TOKEN_TIME_DEV;
        // 로컬이나 개발환경에서는 세션유지 오래 되게 조건 추가

        AccessTokenDto auth = new AccessTokenDto();

        MemberAccount account = MemberAccount.builder()
                .id(admin.getId())
                .expireMinutes(expireMinutes)
                .ip(ip)
                .build();

        AccessTokenUtils.makeAuthToken(account, jwtProperties);

        auth.setAccessToken(account.getAccessToken());
        auth.setRefreshToken(account.getRefreshToken());

        return auth;
    }

    public AccessTokenDto checkMemberRefreshToken(String refreshToken) {
        MemberAccount memberAccount = AccessTokenUtils.checkRefreshToken(refreshToken, jwtProperties);
        return AccessTokenDto.builder()
                .accessToken(memberAccount.getAccessToken())
                .refreshToken(memberAccount.getRefreshToken())
                .build();
    }
}
