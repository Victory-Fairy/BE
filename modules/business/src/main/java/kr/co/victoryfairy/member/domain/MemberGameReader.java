package kr.co.victoryfairy.member.domain;

import java.util.List;
import kr.co.victoryfairy.diary.domain.DiaryEnum;
import kr.co.victoryfairy.game.domain.MatchEnum;

public interface MemberGameReader {

    record Record(DiaryEnum.ViewType viewType, MatchEnum.ResultType result) {
    }

    List<Record> findByMemberAndSeason(Long memberId, String season);
}
