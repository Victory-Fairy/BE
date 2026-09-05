package kr.co.victoryfairy.game.infrastructure.persistence.repository;

import kr.co.victoryfairy.game.infrastructure.persistence.entity.PitcherRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PitcherRecordRepository extends JpaRepository<PitcherRecordEntity, Integer> {

    List<PitcherRecordEntity> findByGameMatchEntityId(String id);

}
