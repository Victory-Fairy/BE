package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.PitcherRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PitcherRecordRepository extends JpaRepository<PitcherRecordEntity, Integer> {

    List<PitcherRecordEntity> findByGameMatchEntityId(String id);

}
