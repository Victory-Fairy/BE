package kr.co.victoryfairy.support.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secretKey;

    /**
     * Access Token 만료 시간 (분)
     */
    private int accessTokenExpireMinutes;

    /**
     * Refresh Token 만료 시간 (일)
     */
    private int refreshTokenExpireDays;

}
