package kr.co.victoryfairy.member.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class JwtServiceAdminTokenTest {

    private final JwtProperties properties = properties();

    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);

    private final JwtService service = new JwtService(properties, refreshTokens);

    @BeforeEach
    void bindRequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void issuesAdminAccessTokenWithAdminRole() {
        var tokens = service.makeAccessToken(AuthModel.AdminDto.builder().id(1L).build());

        var account = AccessTokenCodec.parseRefreshToken(tokens.getAccessToken(), properties);

        assertThat(account.getRoles()).containsExactly("ADMIN");
    }

    @Test
    void addsAdminRoleWhenRotatingLegacyAdminRefreshToken() {
        var legacy = MemberAccount.builder().id(1L).expireMinutes("30").build();
        var refreshToken = JwtCodec.generateToken(Map.of("accountByToken", legacy), 60, properties.getSecretKey());
        when(refreshTokens.validateAdmin(1L, refreshToken)).thenReturn(true);

        var tokens = service.checkAdminRefreshToken(refreshToken);
        var account = AccessTokenCodec.parseRefreshToken(tokens.getAccessToken(), properties);

        assertThat(account.getRoles()).containsExactly("ADMIN");
    }

    private static JwtProperties properties() {
        var properties = new JwtProperties();
        properties.setSecretKey("MDEyMzQ1Njc4OWFiY2RlZg==ZmVkY2JhOTg3NjU0MzIxMA==");
        properties.setAccessTokenExpireMinutes(30);
        properties.setRefreshTokenExpireDays(7);
        return properties;
    }

}
