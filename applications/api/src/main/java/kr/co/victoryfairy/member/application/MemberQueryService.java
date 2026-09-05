package kr.co.victoryfairy.member.application;

import kr.co.victoryfairy.diary.domain.ViewingRecordReader;
import kr.co.victoryfairy.diary.domain.ViewingStatistics;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.member.domain.MemberEnum;
import kr.co.victoryfairy.member.domain.MemberStore;
import kr.co.victoryfairy.member.presentation.MemberDomain;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberStore memberStore;

    private final ViewingRecordReader gameRecordReader;

    public MemberDomain.MemberCheckNickDuplicateResponse checkNickNmDuplicate(String nickNm) {
        authenticatedMemberId();
        if (memberStore.findProfileByNickname(nickNm).isPresent()) {
            return new MemberDomain.MemberCheckNickDuplicateResponse(MemberEnum.NickStatus.DUPLICATE, "중복된 닉네임입니다.");
        }
        return new MemberDomain.MemberCheckNickDuplicateResponse(MemberEnum.NickStatus.AVAILABLE, "사용 가능한 닉네임입니다.");
    }

    public MemberDomain.MemberHomeWinRateResponse findHomeWinRate() {
        var id = authenticatedMemberId();
        if (!memberStore.memberExists(id))
            throw new CustomException(MessageEnum.Data.FAIL_NO_RESULT);
        var recordList = gameRecordReader.findByMemberAndSeason(id, String.valueOf(LocalDate.now().getYear()));
        var result = ViewingStatistics.stadiumResult(recordList);
        if (recordList.isEmpty() || result.win() + result.lose() + result.draw() + result.cancel() == 0) {
            return new MemberDomain.MemberHomeWinRateResponse((short) 0, (short) 0, (short) 0, (short) 0, (short) 0);
        }
        return new MemberDomain.MemberHomeWinRateResponse(result.winAvg(), result.win(), result.lose(), result.draw(),
                result.cancel());
    }

    private Long authenticatedMemberId() {
        var id = CurrentRequest.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }
        return id;
    }

}
