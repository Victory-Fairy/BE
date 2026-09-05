package kr.co.victoryfairy.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.co.victoryfairy.member.domain.Member;
import kr.co.victoryfairy.member.domain.MemberEnum;
import kr.co.victoryfairy.member.domain.MemberProfile;
import kr.co.victoryfairy.member.domain.MemberStore;
import kr.co.victoryfairy.member.infrastructure.oauth.OauthFactory;
import kr.co.victoryfairy.member.infrastructure.security.AuthModel;
import kr.co.victoryfairy.member.infrastructure.security.AccessTokenDto;
import kr.co.victoryfairy.member.infrastructure.security.JwtService;
import kr.co.victoryfairy.member.presentation.MemberDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class MemberAuthServiceTest {

    @BeforeEach
    void bindRequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void existingLoginKeepsMemberIdAndSignupFlags() {
        var store = mock(MemberStore.class);
        var jwt = mock(JwtService.class);
        var member = Member.normal("old", null);
        member = new Member(7L, member.status(), member.lastConnectIp(), member.isUse(), member.createdAt(),
                member.updatedAt(), member.lastConnectAt());
        var profile = new MemberProfile(8L, 7L, 3L, "sns", "mail@test", "nick", MemberEnum.SnsType.APPLE, null, null);
        when(store.findProfile(MemberEnum.SnsType.APPLE, "sns")).thenReturn(Optional.of(profile));
        when(store.findMember(7L)).thenReturn(Optional.of(member));
        when(store.saveMember(any())).thenAnswer(call -> call.getArgument(0));
        when(jwt.makeAccessToken(any(AuthModel.MemberDto.class)))
            .thenReturn(AccessTokenDto.builder().accessToken("access").refreshToken("refresh").build());
        var service = new MemberAuthService(mock(OauthFactory.class), store, jwt);

        var response = service.processLogin(MemberEnum.SnsType.APPLE,
                new MemberDomain.MemberSns(MemberEnum.SnsType.APPLE, "sns", "mail@test"));

        assertThat(response.memberInfo().isNickNmAdded()).isTrue();
        assertThat(response.memberInfo().isTeamAdded()).isTrue();
        verify(store).saveMember(any(Member.class));
    }

    @Test
    void newLoginLinksGeneratedMemberIdToProfile() {
        var store = mock(MemberStore.class);
        var jwt = mock(JwtService.class);
        when(store.findProfile(MemberEnum.SnsType.KAKAO, "sns")).thenReturn(Optional.empty());
        when(store.saveMember(any())).thenAnswer(call -> {
            Member value = call.getArgument(0);
            return new Member(11L, value.status(), value.lastConnectIp(), value.isUse(), value.createdAt(),
                    value.updatedAt(), value.lastConnectAt());
        });
        when(store.saveProfile(any())).thenAnswer(call -> call.getArgument(0));
        when(jwt.makeAccessToken(any(AuthModel.MemberDto.class)))
            .thenReturn(AccessTokenDto.builder().accessToken("access").refreshToken("refresh").build());
        var service = new MemberAuthService(mock(OauthFactory.class), store, jwt);

        var response = service.processLogin(MemberEnum.SnsType.KAKAO,
                new MemberDomain.MemberSns(MemberEnum.SnsType.KAKAO, "sns", "mail@test"));

        assertThat(response.memberInfo().isNickNmAdded()).isFalse();
        verify(store).saveProfile(org.mockito.ArgumentMatchers.argThat(profile -> profile.memberId().equals(11L)));
        verify(jwt).makeAccessToken(org.mockito.ArgumentMatchers.<AuthModel.MemberDto>argThat(
                dto -> dto.getId().equals(11L)));
    }
}
