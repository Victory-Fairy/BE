package kr.co.victoryfairy.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.co.victoryfairy.admin.domain.Admin;
import kr.co.victoryfairy.admin.domain.AdminStore;
import kr.co.victoryfairy.member.infrastructure.security.AccessTokenDto;
import kr.co.victoryfairy.member.infrastructure.security.AuthModel;
import kr.co.victoryfairy.member.infrastructure.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import kr.co.victoryfairy.admin.presentation.AdminAuthDto;
import kr.co.victoryfairy.web.error.CustomException;

class AdminAuthServiceTest {

    @BeforeEach
    void bindRequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void loginMatchesEncodedPasswordIssuesAdminTokenAndUpdatesLogin() {
        var jwt = mock(JwtService.class);
        var encoder = mock(PasswordEncoder.class);
        var admins = mock(AdminStore.class);
        var admin = new Admin(5L, "admin", "encoded", null, true, null, null, null);
        when(admins.findByAdminId("admin")).thenReturn(Optional.of(admin));
        when(encoder.matches("plain", "encoded")).thenReturn(true);
        when(jwt.makeAccessToken(any(AuthModel.AdminDto.class)))
            .thenReturn(AccessTokenDto.builder().accessToken("access").refreshToken("refresh").build());
        var service = new AdminAuthService(jwt, encoder, admins);

        var response = service.login(new AdminAuthDto.LoginRequest("admin", "plain"));

        assertThat(response.accessToken()).isEqualTo("access");
        verify(encoder).matches("plain", "encoded");
        verify(admins).save(any(Admin.class));
        verify(jwt).makeAccessToken(org.mockito.ArgumentMatchers.<AuthModel.AdminDto>argThat(
                dto -> dto.getId().equals(5L)));
    }

    @Test
    void passwordMismatchDoesNotIssueTokenOrUpdateAdmin() {
        var jwt = mock(JwtService.class);
        var encoder = mock(PasswordEncoder.class);
        var admins = mock(AdminStore.class);
        when(admins.findByAdminId("admin"))
            .thenReturn(Optional.of(new Admin(5L, "admin", "encoded", null, true, null, null, null)));
        when(encoder.matches("wrong", "encoded")).thenReturn(false);
        var service = new AdminAuthService(jwt, encoder, admins);

        assertThatThrownBy(() -> service.login(new AdminAuthDto.LoginRequest("admin", "wrong")))
            .isInstanceOf(CustomException.class);

        verify(jwt, never()).makeAccessToken(any(AuthModel.AdminDto.class));
        verify(admins, never()).save(any());
    }
}
