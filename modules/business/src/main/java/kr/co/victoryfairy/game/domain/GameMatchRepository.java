package kr.co.victoryfairy.game.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GameMatchRepository {

    Optional<GameMatch> findById(String id);

    Optional<GameMatch> findDiaryWriteById(String id);

    List<GameMatch> findByDate(LocalDate date, MatchEnum.LeagueType league);

    default List<GameMatch> findByDate(LocalDate date) {
        return findByDate(date, null);
    }

    Optional<GameMatch> findByTeam(Long teamId, LocalDate date);

    List<GameMatch> findAllByTeam(Long teamId, LocalDate date);

    List<GameMatch> findByYearMonth(String year, String month, MatchEnum.LeagueType league);

    List<GameMatch> findBySeason(String season);

    List<GameMatch> findByMatchAt(LocalDateTime matchAt);

    GameMatch save(GameMatch match);

    List<GameMatch> saveAll(List<GameMatch> matches);

    void deleteByLeagueAndSeason(MatchEnum.LeagueType league, String season);

}
