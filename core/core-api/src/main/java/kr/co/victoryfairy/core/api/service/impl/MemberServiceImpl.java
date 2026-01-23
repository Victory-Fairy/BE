package kr.co.victoryfairy.core.api.service.impl;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.MemberEnum;
import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.core.api.domain.MemberDomain;
import kr.co.victoryfairy.core.api.service.MemberService;
import kr.co.victoryfairy.redis.lock.DistributedLock;
import kr.co.victoryfairy.redis.lock.LockName;
import kr.co.victoryfairy.support.model.AuthModel;
import kr.co.victoryfairy.support.service.JwtService;
import kr.co.victoryfairy.core.api.service.oauth.OauthFactory;
import kr.co.victoryfairy.storage.db.core.entity.FileRefEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberInfoEntity;
import kr.co.victoryfairy.storage.db.core.repository.*;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final OauthFactory oauthFactory;

    private final MemberRepository memberRepository;

    private final MemberInfoRepository memberInfoRepository;

    private final TeamRepository teamRepository;

    private final FileRepository fileRepository;

    private final FileRefRepository fileRefRepository;

    private final GameRecordRepository gameRecordRepository;

    private final JwtService jwtService;

    @Lazy
    @Autowired
    private MemberServiceImpl self;

    @Override
    public MemberDomain.MemberOauthPathResponse getOauthPath(MemberEnum.SnsType snsType, String redirectUrl) {
        var service = oauthFactory.getService(snsType);
        var response = service.initSnsAuthPath(redirectUrl);
        log.info("================getOauthPath" + snsType + "=============");
        log.info("response : {}", response);
        return new MemberDomain.MemberOauthPathResponse(response);
    }

    @Override
    public MemberDomain.MemberLoginResponse login(MemberDomain.MemberLoginRequest request) {
        var service = oauthFactory.getService(request.snsType());

        // SNS 정보 가져오기 (외부 API 호출)
        var memberSns = service.parseSnsInfo(request);

        // 분산 락이 적용된 메서드 호출 (self를 통해 호출해야 AOP 적용됨)
        return self.processLogin(request.snsType(), memberSns);
    }

    /**
     * 실제 로그인/회원가입 처리 (분산 락 적용) - 동일 SNS 계정으로 동시 요청 시 중복 가입 방지
     */
    @Transactional
    @DistributedLock(value = LockName.MEMBER_REGISTER, key = "#snsType.name() + '_' + #memberSns.snsId()")
    public MemberDomain.MemberLoginResponse processLogin(MemberEnum.SnsType snsType, MemberDomain.MemberSns memberSns) {
        // sns 정보로 가입된 이력 확인
        var memberInfoEntity = memberInfoRepository.findBySnsTypeAndSnsId(snsType, memberSns.snsId()).orElse(null);

        // memberEntity 없을시 회원 가입 처리
        if (memberInfoEntity == null) {
            MemberEntity memberEntity = MemberEntity.builder()
                .status(MemberEnum.Status.NORMAL)
                .lastConnectIp(RequestUtils.getRemoteIp())
                .lastConnectAt(LocalDateTime.now())
                .build();
            memberRepository.save(memberEntity); // 멤버 등록
            memberInfoEntity = MemberInfoEntity.builder()
                .memberEntity(memberEntity)
                .snsId(memberSns.snsId())
                .snsType(snsType)
                .email(memberSns.email())
                .build();
            memberInfoRepository.save(memberInfoEntity); // 멤버 정보 등록
        }

        // 마지막 로그인 시간, ip 업데이트
        var memberEntity = memberInfoEntity.getMemberEntity();
        memberEntity.updateLastLogin(RequestUtils.getRemoteIp(), LocalDateTime.now());
        memberRepository.save(memberEntity);

        var teamEntity = memberInfoEntity.getTeamEntity();
        var memberInfoDto = AuthModel.MemberInfoDto.builder()
            .snsType(snsType)
            .isNickNmAdded(StringUtils.hasText(memberInfoEntity.getNickNm()))
            .isTeamAdded(teamEntity != null)
            .build();

        var memberDto = AuthModel.MemberDto.builder().id(memberEntity.getId()).memberInfo(memberInfoDto).build();

        var accessTokenDto = jwtService.makeAccessToken(memberDto);
        var memberInfo = new MemberDomain.MemberInfoResponse(snsType, memberSns.snsId(),
                memberInfoDto.getIsNickNmAdded(), memberInfoDto.getIsTeamAdded());
        return new MemberDomain.MemberLoginResponse(memberInfo, accessTokenDto.getAccessToken(),
                accessTokenDto.getRefreshToken());
    }

    @Override
    public void updateTeam(MemberDomain.MemberTeamUpdateRequest request) {
        var id = RequestUtils.getId();
        if (id == null)
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        var memberEntity = memberRepository.findById(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var memberInfoEntity = memberInfoRepository.findByMemberEntity(memberEntity)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var teamEntity = teamRepository.findById(request.teamId())
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        memberInfoEntity = memberInfoEntity.toBuilder().teamEntity(teamEntity).build();

        memberInfoRepository.save(memberInfoEntity);
    }

    @Override
    public MemberDomain.MemberCheckNickDuplicateResponse checkNickNmDuplicate(String nickNm) {
        var id = RequestUtils.getId();
        if (id == null)
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        // DB에 이미 저장된 닉네임인지 체크
        if (memberInfoRepository.findByNickNm(nickNm).isPresent()) {
            return new MemberDomain.MemberCheckNickDuplicateResponse(MemberEnum.NickStatus.DUPLICATE, "중복된 닉네임입니다.");
        }

        return new MemberDomain.MemberCheckNickDuplicateResponse(MemberEnum.NickStatus.AVAILABLE, "사용 가능한 닉네임입니다.");
    }

    @Override
    @Transactional
    public void updateMemberProfile(MemberDomain.MemberProfileUpdateRequest request) {
        var id = RequestUtils.getId();
        if (id == null)
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        // 기존 등록된 프로필 사진 isUse false 처리
        var fileRefEntity = fileRefRepository.findByRefTypeAndRefIdAndIsUseTrue(RefType.PROFILE, id).orElse(null);
        if (fileRefEntity != null) {
            fileRefEntity.delete();
        }

        // TODO file id 로 이미지 path 저장 처리
        if (request.fileId() != null) {
            var fileEntity = fileRepository.findById(request.fileId())
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

            var newFileRefEntity = FileRefEntity.builder()
                .fileEntity(fileEntity)
                .refId(id)
                .refType(RefType.PROFILE)
                .build();

            fileRefRepository.save(newFileRefEntity);
        }
    }

    @Override
    @Transactional
    public void updateMemberNickNm(MemberDomain.MemberNickNmUpdateRequest request) {
        var id = RequestUtils.getId();
        if (id == null)
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        // DB에 이미 저장된 닉네임인지 체크
        if (memberInfoRepository.findByNickNm(request.nickNm()).isPresent()) {
            throw new CustomException(MessageEnum.CheckNick.DUPLICATE);
        }

        var memberEntity = memberRepository.findById(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var memberInfoEntity = memberInfoRepository.findByMemberEntity(memberEntity)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        memberInfoEntity = memberInfoEntity.toBuilder().nickNm(request.nickNm()).build();

        memberInfoRepository.save(memberInfoEntity);
    }

    @Override
    public MemberDomain.MemberHomeWinRateResponse findHomeWinRate() {
        var id = RequestUtils.getId();
        if (id == null)
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        var memberEntity = memberRepository.findById(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var year = String.valueOf(LocalDate.now().getYear());

        var recordList = gameRecordRepository.findByMemberAndSeason(memberEntity, year);

        var stadiumRecord = recordList.stream()
            .filter(record -> record.getViewType() == DiaryEnum.ViewType.STADIUM)
            .toList();

        if (recordList.isEmpty() || stadiumRecord.isEmpty()) {
            return new MemberDomain.MemberHomeWinRateResponse((short) 0, (short) 0, (short) 0, (short) 0, (short) 0);
        }

        var winCount = (short) stadiumRecord.stream()
            .filter(record -> record.getResultType() == MatchEnum.ResultType.WIN)
            .count();

        var loseCount = (short) stadiumRecord.stream()
            .filter(record -> record.getResultType() == MatchEnum.ResultType.LOSS)
            .count();

        var drawCount = (short) stadiumRecord.stream()
            .filter(record -> record.getResultType() == MatchEnum.ResultType.DRAW)
            .count();

        var cancelCount = (short) stadiumRecord.stream()
            .filter(record -> record.getResultType() == MatchEnum.ResultType.CANCEL)
            .count();

        // 승 + 패 경기 수
        var validGameCount = winCount + loseCount;

        // 승률 계산
        short winAvg = 0;
        if (validGameCount > 0) {
            double avg = (double) winCount / validGameCount * 100;
            winAvg = (short) Math.round(avg); // 소수점 첫째자리 반올림
        }

        return new MemberDomain.MemberHomeWinRateResponse(winAvg, (short) winCount, (short) loseCount,
                (short) drawCount, (short) cancelCount);
    }

    @Override
    public MemberDomain.RefreshTokenResponse refreshToken(String refreshToken) {
        var accessTokenDto = jwtService.checkMemberRefreshToken(refreshToken);
        return new MemberDomain.RefreshTokenResponse(accessTokenDto.getAccessToken(), accessTokenDto.getRefreshToken());
    }

    @Override
    public void checkFcmToken(String fcmToken) {
        var id = RequestUtils.getId();

        var memberEntity = memberRepository.findById(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        if (!StringUtils.hasText(memberEntity.getFcmToken()) || !memberEntity.getFcmToken().equals(fcmToken)) {
            memberEntity.updateFcmToken(fcmToken);
            memberRepository.save(memberEntity);
        }

    }

    @Override
    public void logout() {
        var id = RequestUtils.getId();
        if (id == null)
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        jwtService.logout(id);
    }

}