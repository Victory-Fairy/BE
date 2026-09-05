package kr.co.victoryfairy.member.infrastructure.persistence;

import java.util.List;
import kr.co.victoryfairy.member.domain.MemberGameReader;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.GameRecordRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MemberGamePersistenceAdapter implements MemberGameReader {

    private final GameRecordRepository records;
    public MemberGamePersistenceAdapter(GameRecordRepository records) {
        this.records = records;
    }

    public List<Record> findByMemberAndSeason(Long memberId, String season) {
        return records.findPowerByMemberIdAndSeason(memberId, season).stream()
            .map(record -> new Record(record.getViewType(), record.getResultType())).toList();
    }
}
