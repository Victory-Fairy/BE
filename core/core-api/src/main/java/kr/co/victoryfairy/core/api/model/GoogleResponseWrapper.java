package kr.co.victoryfairy.core.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GoogleResponseWrapper {

    @JsonProperty("id")
    private String id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("verified_email")
    private Boolean isEmailVerified;

    @JsonProperty("picture")
    private String picture;

    public String getId() {
        return id;
    }

    public GoogleAccount getGoogleAccount() {
        return new GoogleAccount(email, isEmailVerified);
    }

}
