package kr.co.victoryfairy.core.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KakaoResponseWrapper {

    @JsonProperty("id")
    private String id;

    @JsonProperty("connected_at")
    private String connectedAt;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @JsonProperty("properties")
    private KakaoProperties kakaoProperties;

    public String getId() {
        return id;
    }

    public KakaoAccount getKakaoAccount() {
        return kakaoAccount;
    }

    public KakaoProperties getKakaoProperties() {
        return kakaoProperties;
    }

}
