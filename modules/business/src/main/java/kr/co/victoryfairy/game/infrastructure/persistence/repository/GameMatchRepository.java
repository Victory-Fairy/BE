package kr.co.victoryfairy.game.infrastructure.persistence.repository;

import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GameMatchRepository extends JpaRepository<GameMatchEntity, String> {

    @EntityGraph(attributePaths = { "awayTeamEntity", "homeTeamEntity", "stadiumEntity" })
    Optional<GameMatchEntity> findDiaryWriteById(String id);

    /**
     * 리그 및 시즌 별 경기 삭제
     */
    void deleteByLeagueAndSeason(MatchEnum.LeagueType league, String season);

    /**
     * 시즌 별 경기 일정 불러오기
     * @param sYear
     * @return
     */
    List<GameMatchEntity> findBySeason(String sYear);

    /**
     * @param matchAt
     * @return
     */
    List<GameMatchEntity> findByMatchAt(LocalDateTime matchAt);

}
