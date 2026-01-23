package kr.co.victoryfairy.support.model.oauth;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class MemberAccount {

    private Long id;

    private String ip;

    private String accessToken;

    private String refreshToken;

    private String expireMinutes;

    private List<String> roles;

}
