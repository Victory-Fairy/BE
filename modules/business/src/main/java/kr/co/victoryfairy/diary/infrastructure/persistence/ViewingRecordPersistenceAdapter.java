package kr.co.victoryfairy.diary.infrastructure.persistence;

import java.util.List;
import kr.co.victoryfairy.diary.domain.ViewingRecordReader;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.GameRecordRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ViewingRecordPersistenceAdapter implements ViewingRecordReader {

    private final GameRecordRepository records;
    public ViewingRecordPersistenceAdapter(GameRecordRepository records) {
        this.records = records;
    }

    public List<Record> findByMemberAndSeason(Long memberId, String season) {
        return records.findPowerByMemberIdAndSeason(memberId, season).stream()
            .map(record -> new Record(record.getViewType(), record.getResultType())).toList();
    }
}
