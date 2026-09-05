package kr.co.victoryfairy.member.infrastructure.oauth;

import tools.jackson.databind.ObjectMapper;
import kr.co.victoryfairy.member.domain.MemberEnum;
import kr.co.victoryfairy.member.presentation.MemberDomain;
import kr.co.victoryfairy.member.infrastructure.oauth.model.AuthToken;
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

@Service("KAKAO")
public class KakaoSnsService implements OauthService {

    Logger log = LoggerFactory.getLogger(KakaoSnsService.class);

    @Value("${auth.kakao.cas.client_id}")
    private String kakaoClientId;

    @Value("${auth.kakao.cas.client_secret}")
    private String kakaoClientSecret;

    @Value("${auth.kakao.cas.callback_url}")
    private String kakaoCallbackUrl;

    @Override
    public String initSnsAuthPath(String redirectUrl) {
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
            .queryParam("client_id", kakaoClientId)
            .queryParam("redirect_uri", StringUtils.hasText(redirectUrl) ? redirectUrl : kakaoCallbackUrl)
            .queryParam("response_type", "code")
            .queryParam("prompt", "login")
            .build()
            .toUriString();
    }

    @Override
    public MemberDomain.MemberSns parseSnsInfo(MemberDomain.MemberLoginRequest request) {
        log.info("Kakao OAuth callback received");

        var url = "https://kauth.kakao.com/oauth/token";

        Map<String, String> param = new HashMap<>();
        param.put("grant_type", "authorization_code");
        param.put("client_id", kakaoClientId);
        param.put("client_secret", kakaoClientSecret);
        param.put("redirect_uri", kakaoCallbackUrl);
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

        MemberDomain.MemberSns memberSns = null;

        var kakaoResponse = getUserInfo(tokenResponse.getAccessToken());

        return new MemberDomain.MemberSns(MemberEnum.SnsType.KAKAO, kakaoResponse.getId(),
                kakaoResponse.getKakaoAccount().getEmail());
    }

    private KakaoResponseWrapper getUserInfo(String accessToken) {
        var url = "https://kapi.kakao.com/v2/user/me";

        ObjectMapper mapper = new ObjectMapper();
        String response = OauthHttpClient.doGet(url, null, Map.of("Authorization", "Bearer " + accessToken));
        try {
            return mapper.readValue(response, KakaoResponseWrapper.class);
        }
        catch (Exception e) {
            log.error("Failed to parse KakaoUserInfoResponse", e);
            throw new CustomException(MessageEnum.Auth.FAIL_SNS);
        }
    }

}
