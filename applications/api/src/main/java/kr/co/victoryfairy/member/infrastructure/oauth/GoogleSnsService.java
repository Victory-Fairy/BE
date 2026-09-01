package kr.co.victoryfairy.member.infrastructure.oauth;

import tools.jackson.databind.ObjectMapper;
import io.dodn.springboot.core.enums.MemberEnum;
import kr.co.victoryfairy.member.presentation.MemberDomain;
import kr.co.victoryfairy.member.infrastructure.oauth.model.AuthToken;
import kr.co.victoryfairy.member.infrastructure.oauth.model.GoogleResponseWrapper;
import kr.co.victoryfairy.member.infrastructure.oauth.model.KakaoResponseWrapper;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.member.infrastructure.oauth.OauthHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service("GOOGLE")
public class GoogleSnsService implements OauthService {

    Logger log = LoggerFactory.getLogger(GoogleSnsService.class);

    @Value("${auth.google.cas.client_id}")
    private String googleClientId;

    @Value("${auth.google.cas.client_secret}")
    private String googleClientSecret;

    @Value("${auth.google.cas.callback_url}")
    private String googleCallbackUrl;

    @Override
    public String initSnsAuthPath(String redirectUrl) {
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
            .queryParam("client_id", googleClientId)
            .queryParam("redirect_uri", StringUtils.hasText(redirectUrl) ? redirectUrl : googleCallbackUrl)
            .queryParam("response_type", "code")
            .queryParam("scope", "email openid")
            .build()
            .toUriString();
    }

    @Override
    public MemberDomain.MemberSns parseSnsInfo(MemberDomain.MemberLoginRequest request) {
        log.info("Google OAuth callback received");

        var url = "https://oauth2.googleapis.com/token";

        Map<String, String> param = new HashMap<>();
        param.put("grant_type", "authorization_code");
        param.put("client_id", googleClientId);
        param.put("client_secret", googleClientSecret);
        param.put("redirect_uri", googleCallbackUrl);
        param.put("code", request.code());

        ObjectMapper mapper = new ObjectMapper();
        var response = OauthHttpClient.doPost(url, param);

        AuthToken tokenResponse = null;

        try {
            tokenResponse = mapper.readValue(response, AuthToken.class);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(MessageEnum.Auth.FAIL_SNS);
        }

        var googleResponse = getUserInfo(tokenResponse.getAccessToken());

        return new MemberDomain.MemberSns(MemberEnum.SnsType.KAKAO, googleResponse.getId(),
                googleResponse.getGoogleAccount().getEmail());
    }

    private GoogleResponseWrapper getUserInfo(String accessToken) {
        var url = "https://www.googleapis.com/userinfo/v2/me";

        ObjectMapper mapper = new ObjectMapper();
        String response = OauthHttpClient.doGet(url, null, Map.of("Authorization", "Bearer " + accessToken));
        try {
            return mapper.readValue(response, GoogleResponseWrapper.class);
        }
        catch (Exception e) {
            log.error("Failed to parse KakaoUserInfoResponse", e);
            throw new CustomException(MessageEnum.Auth.FAIL_SNS);
        }
    }

}
