package kr.co.victoryfairy.core.api.model;

public class GoogleAccount {

    private String email;

    private Boolean isEmailVerified;

    public GoogleAccount() {
    }

    public GoogleAccount(String email, Boolean isEmailVerified) {
        this.email = email;
        this.isEmailVerified = isEmailVerified;
    }

    public String getEmail() {
        return email;
    }

}
