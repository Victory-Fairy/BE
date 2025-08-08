package kr.co.victoryfairy.core.api.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.MemberEnum;
import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.core.api.domain.MemberDomain;
import kr.co.victoryfairy.core.api.model.NickNameInfo;
import kr.co.victoryfairy.core.api.service.MemberService;
import kr.co.victoryfairy.core.api.service.oauth.JwtService;
import kr.co.victoryfairy.core.api.service.oauth.OauthFactory;
import kr.co.victoryfairy.storage.db.core.entity.FileRefEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberInfoEntity;
import kr.co.victoryfairy.storage.db.core.repository.*;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
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
    private final RedisHandler redisHandler;

    @Override
    public MemberDomain.MemberOauthPathResponse getOauthPath(MemberEnum.SnsType snsType, String redirectUrl) {
        var service = oauthFactory.getService(snsType);
        var response = service.initSnsAuthPath(redirectUrl);
        log.info("================getOauthPath" + snsType + "=============");
        log.info("response : {}", response);
        return new MemberDomain.MemberOauthPathResponse(response);
    }

    @Override
    @Transactional
    public MemberDomain.MemberLoginResponse login(MemberDomain.MemberLoginRequest request) {
        // TODO : 코드 정리 필요
        var service = oauthFactory.getService(request.snsType());

        // sns 정보 가져오기
        var memberSns = service.parseSnsInfo(request);

        // sns 정보로 가입된 이력 확인
        var memberInfoEntity = memberInfoRepository.findBySnsTypeAndSnsId(request.snsType(), memberSns.snsId())
                .orElse(null);

        // memberEntity 없을시 회원 가입 처리

        MemberDomain.MemberDto memberDto = null;
        if (memberInfoEntity == null) {
            MemberEntity memberEntity = MemberEntity.builder()
                    .status(MemberEnum.Status.NORMAL)
                    .lastConnectIp(RequestUtils.getRemoteIp())
                    .lastConnectAt(LocalDateTime.now())
                    .build();
            memberRepository.save(memberEntity);            // 멤버 등록
            memberInfoEntity = MemberInfoEntity.builder()
                    .memberEntity(memberEntity)
                    .snsId(memberSns.snsId())
                    .snsType(request.snsType())
                    .email(memberSns.email())
                    .build();
            memberInfoRepository.save(memberInfoEntity);    // 멤버 정보 등록

            var memberInfoDto = MemberDomain.MemberInfoDto.builder()
                    .snsType(request.snsType())
                    .isNickNmAdded(false)
                    .isTeamAdded(false)
                    .build();

            memberDto = MemberDomain.MemberDto.builder()
                    .id(memberEntity.getId())
                    .memberInfo(memberInfoDto)
                    .build();
        }

        // 마지막 로그인 시간, ip 업데이트
        var memberEntity = memberInfoEntity.getMemberEntity();
        memberEntity.updateLastLogin(RequestUtils.getRemoteIp(), LocalDateTime.now());
        memberRepository.save(memberEntity);

        var teamEntity = memberInfoEntity.getTeamEntity();
        var memberInfoDto = MemberDomain.MemberInfoDto.builder()
                .snsType(request.snsType())
                .isNickNmAdded(StringUtils.hasText(memberInfoEntity.getNickNm()))
                .isTeamAdded(teamEntity != null)
                .build();
        memberDto = MemberDomain.MemberDto.builder()
                .id(memberEntity.getId())
                .memberInfo(memberInfoDto)
                .build();

        var accessTokenDto = jwtService.makeAccessToken(memberDto);
        var memberInfo = new MemberDomain.MemberInfoResponse(request.snsType(), memberSns.snsId(), memberInfoDto.getIsNickNmAdded(), memberInfoDto.getIsTeamAdded());
        var memberLoginResponse = new MemberDomain.MemberLoginResponse(memberInfo, accessTokenDto.getAccessToken(), accessTokenDto.getRefreshToken());
        return memberLoginResponse;
    }

    @Override
    public void updateTeam(MemberDomain.MemberTeamUpdateRequest request) {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        var memberEntity = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var memberInfoEntity = memberInfoRepository.findByMemberEntity(memberEntity)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var teamEntity = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        memberInfoEntity = memberInfoEntity.toBuilder()
                .teamEntity(teamEntity)
                .build();

        memberInfoRepository.save(memberInfoEntity);
    }

    @Override
    public MemberDomain.MemberCheckNickNameResponse checkNick() {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        var myNick = redisHandler.getHashValue("memberNickNm", String.valueOf(id), NickNameInfo.class);

        if (myNick == null) {
            return new MemberDomain.MemberCheckNickNameResponse(null);
        }

        return new MemberDomain.MemberCheckNickNameResponse(myNick.getKey());
    }

    @Override
    public MemberDomain.MemberCheckNickDuplicateResponse checkNickNmDuplicate(String nickNm) {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        // Redis 에 저장된 nickNm 이 있는지 체크
        var existingNick = redisHandler.getHashValue("checkNick", nickNm, NickNameInfo.class);

        if (existingNick != null) {
            var now = LocalDateTime.now();
            // 선점된지 72시간 이내의 nickNm 인지 체크
            if (Duration.between(existingNick.getCreatedAt(), now).toHours() < 72) {
                return new MemberDomain.MemberCheckNickDuplicateResponse(MemberEnum.NickStatus.DUPLICATE, "중복된 닉네임입니다.");
            }

            // 72시간이 지난 경우 만료로 보고 삭제
            redisHandler.deleteHashValue("checkNick", nickNm);
            redisHandler.deleteHashValue("memberNickNm", String.valueOf(existingNick.getKey()));
        }

        // 이미 DB 에 저장된 닉네임인지 체크
        if (memberInfoRepository.findByNickNm(nickNm).isPresent()) {
            return new MemberDomain.MemberCheckNickDuplicateResponse(MemberEnum.NickStatus.DUPLICATE, "중복된 닉네임입니다.");
        }

        // 이미 선점한 닉네임이 있을 경우 삭제 처리
        var myNickJson = redisHandler.getHashValue("memberNickNm", String.valueOf(id));
        if (StringUtils.hasText(myNickJson)) {
            var myNick = extractKeyFromJson(myNickJson);
            redisHandler.deleteHashValue("memberNickNm", String.valueOf(id));
            redisHandler.deleteHashValue("checkNick", myNick);
        }

        var info = NickNameInfo.builder()
                .key(String.valueOf(id))
                .createdAt(LocalDateTime.now())
                .build();

        var myNickInfo = NickNameInfo.builder()
                .key(nickNm)
                .createdAt(LocalDateTime.now())
                .build();

        redisHandler.setHashValue("checkNick", nickNm, info);
        redisHandler.setHashValue("memberNickNm", String.valueOf(id), myNickInfo);

        return new MemberDomain.MemberCheckNickDuplicateResponse(MemberEnum.NickStatus.AVAILABLE, "사용 가능한 닉네임입니다.");
    }

    @Override
    @Transactional
    public void updateMemberProfile(MemberDomain.MemberProfileUpdateRequest request) {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

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
    public void updateMemberNickNm(MemberDomain.MemberNickNmUpdateRequest request) {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

        var existingNick = redisHandler.getHashValue("checkNick", request.nickNm(), NickNameInfo.class);
        if (existingNick == null) {
            throw new CustomException(MessageEnum.CheckNick.NON_CHECK);
        }
        if (!Long.valueOf(existingNick.getKey()).equals(id)) {
            throw new CustomException(MessageEnum.CheckNick.POSSESSION);
        }

        var memberEntity = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var memberInfoEntity = memberInfoRepository.findByMemberEntity(memberEntity)
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        memberInfoEntity = memberInfoEntity.toBuilder()
                .nickNm(request.nickNm())
                .build();

        memberInfoRepository.save(memberInfoEntity);

        // Redis 에 저장된 데이터 삭제 처리
        redisHandler.deleteHashValue("checkNick", request.nickNm());
        redisHandler.deleteHashValue("memberNickNm", String.valueOf(id));
    }

    @Override
    public MemberDomain.MemberHomeWinRateResponse findHomeWinRate() {
        var id = RequestUtils.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);

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
            winAvg = (short) Math.round(avg);  // 소수점 첫째자리 반올림
        }

        return new MemberDomain.MemberHomeWinRateResponse(
                winAvg,
                (short) winCount,
                (short) loseCount,
                (short) drawCount,
                (short) cancelCount
        );
    }

    private String extractKeyFromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);
            return node.get("key").asText();
        } catch (Exception e) {
            throw new RuntimeException("Invalid json format", e);
        }
    }
}