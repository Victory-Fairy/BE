package kr.co.victoryfairy.game.infrastructure.persistence.repository;

import kr.co.victoryfairy.game.infrastructure.persistence.entity.SeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<SeatEntity, Long> {

    List<SeatEntity> findByStadiumEntityIdAndSeason(Long id, String season);

}
