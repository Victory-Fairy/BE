package kr.co.victoryfairy.game.infrastructure.persistence.repository;

import kr.co.victoryfairy.game.infrastructure.persistence.entity.StadiumEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StadiumRepository extends JpaRepository<StadiumEntity, Long> {

    Optional<StadiumEntity> findByExternalId(Integer externalId);

}
