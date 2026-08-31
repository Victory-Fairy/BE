package kr.co.victoryfairy.member.application;

import io.dodn.springboot.core.enums.MemberEnum;
import kr.co.victoryfairy.member.presentation.MemberDomain;
import kr.co.victoryfairy.member.infrastructure.oauth.OauthFactory;
import kr.co.victoryfairy.redis.lock.DistributedLock;
import kr.co.victoryfairy.redis.lock.LockName;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberInfoEntity;
import kr.co.victoryfairy.storage.db.core.repository.MemberInfoRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberRepository;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.member.infrastructure.security.AuthModel;
import kr.co.victoryfairy.member.infrastructure.security.JwtService;
import kr.co.victoryfairy.member.infrastructure.security.RequestUtils;
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

    private final MemberRepository memberRepository;

    private final MemberInfoRepository memberInfoRepository;

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
        var memberInfoEntity = memberInfoRepository.findBySnsTypeAndSnsId(snsType, memberSns.snsId()).orElse(null);

        if (memberInfoEntity == null) {
            MemberEntity memberEntity = MemberEntity.builder()
                .status(MemberEnum.Status.NORMAL)
                .lastConnectIp(RequestUtils.getRemoteIp())
                .lastConnectAt(LocalDateTime.now())
                .build();
            memberRepository.save(memberEntity);
            memberInfoEntity = MemberInfoEntity.builder()
                .memberEntity(memberEntity)
                .snsId(memberSns.snsId())
                .snsType(snsType)
                .email(memberSns.email())
                .build();
            memberInfoRepository.save(memberInfoEntity);
        }

        var memberEntity = memberInfoEntity.getMemberEntity();
        memberEntity.updateLastLogin(RequestUtils.getRemoteIp(), LocalDateTime.now());
        memberRepository.save(memberEntity);

        var memberInfoDto = AuthModel.MemberInfoDto.builder()
            .isNickNmAdded(StringUtils.hasText(memberInfoEntity.getNickNm()))
            .isTeamAdded(memberInfoEntity.getTeamEntity() != null)
            .build();

        var memberDto = AuthModel.MemberDto.builder().id(memberEntity.getId()).memberInfo(memberInfoDto).build();
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
        var id = RequestUtils.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }
        jwtService.logout(id);
    }

}
