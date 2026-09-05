package kr.co.victoryfairy.game.infrastructure.persistence.repository;

import kr.co.victoryfairy.game.infrastructure.persistence.entity.HitterRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HitterRecordRepository extends JpaRepository<HitterRecordEntity, Integer> {

    List<HitterRecordEntity> findByGameMatchEntityId(String id);

}
