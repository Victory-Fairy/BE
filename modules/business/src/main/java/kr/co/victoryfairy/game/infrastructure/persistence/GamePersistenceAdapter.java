package kr.co.victoryfairy.game.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import kr.co.victoryfairy.game.domain.GameMatch;
import kr.co.victoryfairy.game.domain.GameMatchRepository;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchCustomRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.StadiumRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.TeamRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class GamePersistenceAdapter implements GameMatchRepository {

    private final kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchRepository matches;

    private final GameMatchCustomRepository queries;

    private final TeamRepository teams;

    private final StadiumRepository stadiums;

    public GamePersistenceAdapter(
            kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchRepository matches,
            GameMatchCustomRepository queries, TeamRepository teams, StadiumRepository stadiums) {
        this.matches = matches;
        this.queries = queries;
        this.teams = teams;
        this.stadiums = stadiums;
    }

    public Optional<GameMatch> findById(String id) {
        return matches.findById(id).map(GamePersistenceMapper::toDomain);
    }

    public List<GameMatch> findByDate(LocalDate date, MatchEnum.LeagueType league) {
        return queries.findByMatchAt(date, league).stream().map(GamePersistenceMapper::toDomain).toList();
    }

    public Optional<GameMatch> findByTeam(Long teamId, LocalDate date) {
        return queries.findByTeamId(teamId, date).map(GamePersistenceMapper::toDomain);
    }

    public List<GameMatch> findAllByTeam(Long teamId, LocalDate date) {
        return queries.findByTeamIdIn(teamId, date).stream().map(GamePersistenceMapper::toDomain).toList();
    }

    public List<GameMatch> findByYearMonth(String year, String month, MatchEnum.LeagueType league) {
        return queries.findByYearAndMonthAndEqLeague(year, month, league)
            .stream()
            .map(GamePersistenceMapper::toDomain)
            .toList();
    }

    public List<GameMatch> findBySeason(String season) {
        return matches.findBySeason(season).stream().map(GamePersistenceMapper::toDomain).toList();
    }

    public List<GameMatch> findByMatchAt(LocalDateTime matchAt) {
        return matches.findByMatchAt(matchAt).stream().map(GamePersistenceMapper::toDomain).toList();
    }

    @Transactional
    public GameMatch save(GameMatch value) {
        GameMatchEntity entity = matches.findById(value.id())
            .orElseGet(() -> GameMatchEntity.builder().id(value.id()).build());
        entity.apply(value, value.awayTeamId() == null ? null : teams.getReferenceById(value.awayTeamId()),
                value.homeTeamId() == null ? null : teams.getReferenceById(value.homeTeamId()),
                value.stadiumId() == null ? null : stadiums.getReferenceById(value.stadiumId()));
        return GamePersistenceMapper.toDomain(matches.save(entity));
    }

    @Transactional
    public List<GameMatch> saveAll(List<GameMatch> values) {
        var existing = matches.findAllById(values.stream().map(GameMatch::id).toList())
            .stream()
            .collect(Collectors.toMap(GameMatchEntity::getId, Function.identity()));
        var entities = values.stream().map(value -> {
            var entity = existing.get(value.id());
            var stored = entity == null ? value : GamePersistenceMapper.toDomain(entity).syncSchedule(value);
            if (entity == null)
                entity = GameMatchEntity.builder().id(value.id()).build();
            entity.apply(stored, stored.awayTeamId() == null ? null : teams.getReferenceById(stored.awayTeamId()),
                    stored.homeTeamId() == null ? null : teams.getReferenceById(stored.homeTeamId()),
                    stored.stadiumId() == null ? null : stadiums.getReferenceById(stored.stadiumId()));
            return entity;
        }).toList();
        return matches.saveAll(entities).stream().map(GamePersistenceMapper::toDomain).toList();
    }

    public void deleteByLeagueAndSeason(MatchEnum.LeagueType league, String season) {
        matches.deleteByLeagueAndSeason(league, season);
    }

}
