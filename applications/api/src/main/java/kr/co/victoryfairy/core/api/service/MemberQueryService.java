package kr.co.victoryfairy.core.api.service;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.MemberEnum;
import kr.co.victoryfairy.core.api.domain.MemberDomain;
import kr.co.victoryfairy.storage.db.core.entity.GameRecordEntity;
import kr.co.victoryfairy.storage.db.core.repository.GameRecordRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberInfoRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberRepository;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberRepository memberRepository;

    private final MemberInfoRepository memberInfoRepository;

    private final GameRecordRepository gameRecordRepository;

    public MemberDomain.MemberCheckNickDuplicateResponse checkNickNmDuplicate(String nickNm) {
        authenticatedMemberId();
        if (memberInfoRepository.findByNickNm(nickNm).isPresent()) {
            return new MemberDomain.MemberCheckNickDuplicateResponse(MemberEnum.NickStatus.DUPLICATE, "중복된 닉네임입니다.");
        }
        return new MemberDomain.MemberCheckNickDuplicateResponse(MemberEnum.NickStatus.AVAILABLE, "사용 가능한 닉네임입니다.");
    }

    public MemberDomain.MemberHomeWinRateResponse findHomeWinRate() {
        var id = authenticatedMemberId();
        var memberEntity = memberRepository.findById(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));
        var recordList = gameRecordRepository.findByMemberAndSeason(memberEntity,
                String.valueOf(LocalDate.now().getYear()));
        var stadiumRecord = recordList.stream()
            .filter(record -> record.getViewType() == DiaryEnum.ViewType.STADIUM)
            .toList();

        if (recordList.isEmpty() || stadiumRecord.isEmpty()) {
            return new MemberDomain.MemberHomeWinRateResponse((short) 0, (short) 0, (short) 0, (short) 0, (short) 0);
        }

        var winCount = count(stadiumRecord, MatchEnum.ResultType.WIN);
        var loseCount = count(stadiumRecord, MatchEnum.ResultType.LOSS);
        var drawCount = count(stadiumRecord, MatchEnum.ResultType.DRAW);
        var cancelCount = count(stadiumRecord, MatchEnum.ResultType.CANCEL);
        var validGameCount = winCount + loseCount;
        short winAvg = validGameCount == 0 ? 0 : (short) Math.round((double) winCount / validGameCount * 100);

        return new MemberDomain.MemberHomeWinRateResponse(winAvg, winCount, loseCount, drawCount, cancelCount);
    }

    private short count(List<GameRecordEntity> records, MatchEnum.ResultType result) {
        return (short) records.stream().filter(record -> record.getResultType() == result).count();
    }

    private Long authenticatedMemberId() {
        var id = RequestUtils.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }
        return id;
    }

}
