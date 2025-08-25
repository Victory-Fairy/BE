package kr.co.victoryfairy.core.api.service.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dodn.springboot.core.enums.MemberEnum;
import kr.co.victoryfairy.core.api.domain.MemberDomain;
import kr.co.victoryfairy.core.api.model.AuthToken;
import kr.co.victoryfairy.core.api.model.KakaoResponseWrapper;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.utils.HttpClientUtils;
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
        return UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", kakaoClientId)
                .queryParam("redirect_uri", StringUtils.hasText(redirectUrl) ? redirectUrl : kakaoCallbackUrl)
                .queryParam("response_type", "code")
                .queryParam("prompt", "login")
                .build()
                .toUriString();
    }

    @Override
    public MemberDomain.MemberSns parseSnsInfo(MemberDomain.MemberLoginRequest request) {
        log.info("================kakaoCallback=============");
        log.info("code: {}}", request.code());
        log.info("orgCallbackUrl: {} , state: {}", kakaoCallbackUrl);

        var url = "https://kauth.kakao.com/oauth/token";

        Map<String, String> param = new HashMap<>();
        param.put("grant_type", "authorization_code");
        param.put("client_id", kakaoClientId);
        param.put("client_secret", kakaoClientSecret);
        param.put("redirect_uri", kakaoCallbackUrl);
        param.put("code", request.code());

        ObjectMapper mapper = new ObjectMapper();
        var response = HttpClientUtils.doPost(url , param);

        log.info("response : {}", response);
        AuthToken tokenResponse = null;

        try {
            tokenResponse = mapper.readValue(response, AuthToken.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(MessageEnum.Auth.FAIL_SNS);
        }

        MemberDomain.MemberSns memberSns = null;

        var kakaoResponse = getUserInfo(tokenResponse.getAccessToken());

        return new MemberDomain.MemberSns(MemberEnum.SnsType.KAKAO, kakaoResponse.getId(), kakaoResponse.getKakaoAccount().getEmail());
    }

    private KakaoResponseWrapper getUserInfo(String accessToken) {
        var url = "https://kapi.kakao.com/v2/user/me";

        ObjectMapper mapper = new ObjectMapper();
        String response = HttpClientUtils.doGet(url, null, Map.of("Authorization", "Bearer " + accessToken));
        log.warn("response:*******" + response + "*******");

        try {
            // JSON 데이터를 NaverUserResponseWrapper 객체로 매핑
            KakaoResponseWrapper wrapper = mapper.readValue(response, KakaoResponseWrapper.class);
            log.warn("wrapper: " + wrapper);

            return wrapper;
        } catch (Exception e) {
            log.error("Failed to parse KakaoUserInfoResponse", e);
            throw new CustomException(MessageEnum.Auth.FAIL_SNS);
        }
    }
}
