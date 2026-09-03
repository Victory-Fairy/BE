package kr.co.victoryfairy.member.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import kr.co.victoryfairy.web.error.CustomException;

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

    @Test
    void rejectsMemberTokenOnAdminPath() {
        var properties = properties();
        var account = MemberAccount.builder().id(787L).expireMinutes("30").roles(List.of("USER")).build();
        AccessTokenCodec.makeAuthToken(account, properties, 7);
        var request = request("/v2/admin/community/reports", account.getAccessToken());

        assertThatThrownBy(() -> AccessTokenCodec.checkToken(request, properties))
            .isInstanceOf(CustomException.class);
    }

    @Test
    void acceptsAdminTokenOnAdminPath() {
        var properties = properties();
        var account = MemberAccount.builder().id(1L).expireMinutes("30").roles(List.of("ADMIN")).build();
        AccessTokenCodec.makeAuthToken(account, properties, 7);
        var request = request("/v2/admin/community/reports", account.getAccessToken());

        assertThat(AccessTokenCodec.checkToken(request, properties)).isTrue();
    }

    private JwtProperties properties() {
        var properties = new JwtProperties();
        properties.setSecretKey("MDEyMzQ1Njc4OWFiY2RlZg==ZmVkY2JhOTg3NjU0MzIxMA==");
        return properties;
    }

    private MockHttpServletRequest request(String uri, String accessToken) {
        var request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        request.addHeader("Authorization", "Bearer " + accessToken);
        return request;
    }

}
