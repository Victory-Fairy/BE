package kr.co.victoryfairy.member.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

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

}
