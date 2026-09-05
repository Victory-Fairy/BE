package kr.co.victoryfairy.member.application;

import kr.co.victoryfairy.member.domain.MemberEnum;
import kr.co.victoryfairy.member.domain.Member;
import kr.co.victoryfairy.member.domain.MemberProfile;
import kr.co.victoryfairy.member.domain.MemberStore;
import kr.co.victoryfairy.member.presentation.MemberDomain;
import kr.co.victoryfairy.member.infrastructure.oauth.OauthFactory;
import kr.co.victoryfairy.redis.lock.DistributedLock;
import kr.co.victoryfairy.redis.lock.LockName;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.member.infrastructure.security.AuthModel;
import kr.co.victoryfairy.member.infrastructure.security.JwtService;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAuthService {

    private final OauthFactory oauthFactory;

    private final MemberStore memberStore;

    private final JwtService jwtService;

    @Lazy
    @Autowired
    private MemberAuthService self;

    public MemberDomain.MemberOauthPathResponse getOauthPath(MemberEnum.SnsType snsType, String redirectUrl) {
        var service = oauthFactory.getService(snsType);
        var response = service.initSnsAuthPath(redirectUrl);
        log.info("OAuth path created for {}", snsType);
        return new MemberDomain.MemberOauthPathResponse(response);
    }

    public MemberDomain.MemberLoginResponse login(MemberDomain.MemberLoginRequest request) {
        var service = oauthFactory.getService(request.snsType());
        var memberSns = service.parseSnsInfo(request);
        return self.processLogin(request.snsType(), memberSns);
    }

    @Transactional
    @DistributedLock(value = LockName.MEMBER_REGISTER, key = "#snsType.name() + '_' + #memberSns.snsId()")
    public MemberDomain.MemberLoginResponse processLogin(MemberEnum.SnsType snsType, MemberDomain.MemberSns memberSns) {
        var memberProfile = memberStore.findProfile(snsType, memberSns.snsId()).orElse(null);
        Member member;

        if (memberProfile == null) {
            member = memberStore.saveMember(Member.normal(CurrentRequest.getRemoteIp(), LocalDateTime.now()));
            memberProfile = memberStore.saveProfile(MemberProfile.social(member.id(), snsType, memberSns.snsId(),
                    memberSns.email()));
        }
        else {
            member = memberStore.findMember(memberProfile.memberId())
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));
        }

        member = memberStore.saveMember(member.login(CurrentRequest.getRemoteIp(), LocalDateTime.now()));

        var memberInfoDto = AuthModel.MemberInfoDto.builder()
            .isNickNmAdded(StringUtils.hasText(memberProfile.nickNm()))
            .isTeamAdded(memberProfile.teamId() != null)
            .build();

        var memberDto = AuthModel.MemberDto.builder().id(member.id()).memberInfo(memberInfoDto).build();
        var accessTokenDto = jwtService.makeAccessToken(memberDto);
        var memberInfo = new MemberDomain.MemberInfoResponse(snsType, memberSns.snsId(),
                memberInfoDto.getIsNickNmAdded(), memberInfoDto.getIsTeamAdded());
        return new MemberDomain.MemberLoginResponse(memberInfo, accessTokenDto.getAccessToken(),
                accessTokenDto.getRefreshToken());
    }

    public MemberDomain.RefreshTokenResponse refreshToken(String refreshToken) {
        var accessTokenDto = jwtService.checkMemberRefreshToken(refreshToken);
        return new MemberDomain.RefreshTokenResponse(accessTokenDto.getAccessToken(), accessTokenDto.getRefreshToken());
    }

    public void logout() {
        var id = CurrentRequest.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }
        jwtService.logout(id);
    }

}
