package kr.co.victoryfairy.game.infrastructure.persistence.repository;

import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {

    List<TeamEntity> findAllByOrderByOrderNo();

    List<TeamEntity> findByLeagueOrderByOrderNo(MatchEnum.LeagueType league);

    Optional<TeamEntity> findByCountryCode(String countryCode);

}
