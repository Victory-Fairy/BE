package kr.co.victoryfairy.diary.infrastructure.persistence.repository;

import kr.co.victoryfairy.diary.infrastructure.persistence.entity.SeatReviewEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.SeatUseHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatReviewRepository extends JpaRepository<SeatReviewEntity, Long> {

    List<SeatReviewEntity> findBySeatUseHistoryEntity(SeatUseHistoryEntity id);

    List<SeatReviewEntity> findAllBySeatUseHistoryEntityIdIn(List<Long> ids);

}
