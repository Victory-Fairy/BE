package kr.co.victoryfairy.diary.domain;

import java.util.List;
import java.util.Optional;

public interface GameRecordStore {
    Optional<GameRecord> findByDiaryId(Long diaryId);
    List<GameRecord> findByMemberAndSeasonOrdered(Long memberId, String season);
    GameRecord save(GameRecord record);
    void delete(Long id);
}
