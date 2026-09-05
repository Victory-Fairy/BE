package kr.co.victoryfairy.game.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import kr.co.victoryfairy.game.domain.Team;
import kr.co.victoryfairy.game.domain.TeamReader;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.TeamRepository;
import org.springframework.stereotype.Repository;

@Repository
public class GameReferenceDataAdapter implements TeamReader {

    private final TeamRepository teams;

    public GameReferenceDataAdapter(TeamRepository teams) {
        this.teams = teams;
    }

    public Optional<Team> findById(Long id) {
        return teams.findById(id).map(GamePersistenceMapper::toDomain);
    }

    public List<Team> findAllById(Collection<Long> ids) {
        return teams.findAllById(ids).stream().map(GamePersistenceMapper::toDomain).toList();
    }

    public List<Team> findAll() {
        return teams.findAll().stream().map(GamePersistenceMapper::toDomain).toList();
    }

    public Optional<Team> findByCountryCode(String code) {
        return teams.findByCountryCode(code).map(GamePersistenceMapper::toDomain);
    }

    public List<Team> findAllOrdered() {
        return teams.findAllByOrderByOrderNo().stream().map(GamePersistenceMapper::toDomain).toList();
    }

    public List<Team> findByLeagueOrdered(kr.co.victoryfairy.game.domain.MatchEnum.LeagueType league) {
        return teams.findByLeagueOrderByOrderNo(league).stream().map(GamePersistenceMapper::toDomain).toList();
    }

}
