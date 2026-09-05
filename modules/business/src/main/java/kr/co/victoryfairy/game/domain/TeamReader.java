package kr.co.victoryfairy.game.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeamReader {

    Optional<Team> findById(Long id);

    List<Team> findAllById(Collection<Long> ids);

    List<Team> findAll();

    Optional<Team> findByCountryCode(String countryCode);

    List<Team> findAllOrdered();

    List<Team> findByLeagueOrdered(MatchEnum.LeagueType league);

}
