package kr.co.victoryfairy.core.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KakaoAccount {

    @JsonProperty("profile_nickname_needs_agreement")
    private Boolean profileNicknameNeedsAgreement;

    @JsonProperty("email")
    private String email;

    @JsonProperty("profile")
    private Profile profile;

    @JsonProperty("has_email")
    private Boolean hasEmail;

    @JsonProperty("email_needs_agreement")
    private Boolean emailNeedsAgreement;

    @JsonProperty("is_email_valid")
    private Boolean isEmailValid;

    @JsonProperty("is_email_verified")
    private Boolean isEmailVerified;

    public String getEmail() {
        return email;
    }

}

class Profile {

    @JsonProperty("nickname")
    String nickname;

    @JsonProperty("is_default_nickname")
    Boolean isDefaultNickname;

}