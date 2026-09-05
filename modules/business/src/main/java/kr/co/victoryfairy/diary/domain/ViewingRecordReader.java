package kr.co.victoryfairy.diary.domain;

import java.util.List;
import kr.co.victoryfairy.game.domain.MatchEnum;

public interface ViewingRecordReader {
    record Record(DiaryEnum.ViewType viewType, MatchEnum.ResultType result) {}

    List<Record> findByMemberAndSeason(Long memberId, String season);
}
