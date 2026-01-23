package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.FreeDiaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FreeDiaryRepository extends JpaRepository<FreeDiaryEntity, Long> {

    List<FreeDiaryEntity> findByMemberId(Long memberId);

    Optional<FreeDiaryEntity> findByMemberIdAndId(Long memberId, Long id);

    // 월별 조회
    List<FreeDiaryEntity> findByMemberIdAndMatchAtBetween(Long memberId, LocalDateTime startAt, LocalDateTime endAt);

}