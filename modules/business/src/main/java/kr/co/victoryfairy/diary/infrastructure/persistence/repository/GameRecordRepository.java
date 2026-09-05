package kr.co.victoryfairy.diary.infrastructure.persistence.repository;

import kr.co.victoryfairy.diary.domain.DiaryEnum;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.GameRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRecordRepository extends JpaRepository<GameRecordEntity, Long> {

    interface PowerView {
        DiaryEnum.ViewType getViewType();
        MatchEnum.ResultType getResultType();
    }

    @Query("select g.viewType as viewType, g.resultType as resultType from game_record g "
            + "where g.memberId = :memberId and g.season = :season")
    List<PowerView> findPowerByMemberIdAndSeason(@Param("memberId") Long memberId, @Param("season") String season);

    @EntityGraph(attributePaths = { "gameMatchEntity", "gameMatchEntity.homeTeamEntity", "stadiumEntity",
            "teamEntity" })
    List<GameRecordEntity> findByMemberIdAndSeasonOrderByGameMatchEntityMatchAtAsc(Long memberId, String season);

    List<GameRecordEntity> findByMemberId(Long memberId);

    Optional<GameRecordEntity> findByDiaryEntityId(Long diaryId);

    // 리그 타입별 조회 메서드
    List<GameRecordEntity> findByMemberIdAndSeasonAndLeagueType(Long memberId, String season,
            MatchEnum.LeagueType leagueType);

    List<GameRecordEntity> findByMemberIdAndLeagueType(Long memberId, MatchEnum.LeagueType leagueType);

}
