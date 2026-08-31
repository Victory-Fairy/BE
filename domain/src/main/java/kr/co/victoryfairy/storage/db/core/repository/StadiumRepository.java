package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.StadiumEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StadiumRepository extends JpaRepository<StadiumEntity, Long> {

    Optional<StadiumEntity> findByExternalId(Integer externalId);

}
