package kr.co.victoryfairy.diary.domain;

import java.util.List;
import java.util.Optional;

public interface DiaryStore {
    Optional<Diary> findByMemberAndMatch(Long memberId, String matchId);
    Optional<Diary> findByMemberAndId(Long memberId, Long diaryId);
    Optional<Diary> findDetailByMemberAndId(Long memberId, Long diaryId);
    List<Diary> findUnratedByMatch(String matchId);
    List<Diary> findAllUnratedTerminal();
    Diary save(Diary diary);
    void delete(Long diaryId);
}
