package kr.co.victoryfairy.diary.domain;

import java.util.Optional;
import java.util.List;
import java.util.Map;

public interface SeatUseStore {
    record SeatUse(Long seatId, String name) {}
    void save(Long diaryId, Long seatId, String name);
    void replace(Long diaryId, Long seatId, String name);
    void delete(Long diaryId);
    Optional<SeatUse> find(Long diaryId);
    Map<Long, List<String>> findDescriptions(List<Long> diaryIds);
}
