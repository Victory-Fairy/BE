package kr.co.victoryfairy.storage.db.core.repository;

import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {

    List<TeamEntity> findAllByOrderByOrderNo();

    List<TeamEntity> findByLeagueOrderByOrderNo(MatchEnum.LeagueType league);

    Optional<TeamEntity> findByCountryCode(String countryCode);

}
