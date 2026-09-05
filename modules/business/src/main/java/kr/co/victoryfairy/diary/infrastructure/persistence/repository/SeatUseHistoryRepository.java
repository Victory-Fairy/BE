package kr.co.victoryfairy.diary.infrastructure.persistence.repository;

import kr.co.victoryfairy.diary.infrastructure.persistence.entity.SeatUseHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatUseHistoryRepository extends JpaRepository<SeatUseHistoryEntity, Long> {

    SeatUseHistoryEntity findByDiaryEntityId(Long diaryId);

    List<SeatUseHistoryEntity> findAllByDiaryEntityIdIn(List<Long> diaryId);

}
