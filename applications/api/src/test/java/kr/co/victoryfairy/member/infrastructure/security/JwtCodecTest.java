package kr.co.victoryfairy.member.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JwtCodecTest {

    @Test
    void supportsLegacyConcatenatedBase64Secrets() {
        String secret = "MDEyMzQ1Njc4OWFiY2RlZg==ZmVkY2JhOTg3NjU0MzIxMA==";

        String token = JwtCodec.generateToken(Map.of("memberId", 1L), 5, secret);

        assertThat(token).isNotNull();
        assertThat(JwtCodec.parseToken(token, secret).get("isCertifiedToken")).isEqualTo(true);
    }

    @Test
    void roundTripsMemberAccountThroughRefreshToken() {
        String secret = "MDEyMzQ1Njc4OWFiY2RlZg==ZmVkY2JhOTg3NjU0MzIxMA==";
        var properties = new JwtProperties();
        properties.setSecretKey(secret);
        var account = MemberAccount.builder()
            .id(787L)
            .expireMinutes("30")
            .roles(List.of("USER"))
            .build();

        String token = JwtCodec.generateToken(Map.of("accountByToken", account), 5, secret);
        MemberAccount parsed = AccessTokenCodec.parseRefreshToken(token, properties);

        assertThat(parsed.getId()).isEqualTo(787L);
        assertThat(parsed.getRoles()).containsExactly("USER");
    }

}
