package kr.co.victoryfairy.member.infrastructure.persistence;

import java.util.List;
import kr.co.victoryfairy.member.domain.MemberGameReader;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.GameRecordRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MemberGamePersistenceAdapter implements MemberGameReader {

    private final GameRecordRepository records;
    private final MemberRepository members;

    public MemberGamePersistenceAdapter(GameRecordRepository records, MemberRepository members) {
        this.records = records;
        this.members = members;
    }

    public List<Record> findByMemberAndSeason(Long memberId, String season) {
        return records.findByMemberAndSeason(members.getReferenceById(memberId), season).stream()
            .map(record -> new Record(record.getViewType(), record.getResultType())).toList();
    }
}
