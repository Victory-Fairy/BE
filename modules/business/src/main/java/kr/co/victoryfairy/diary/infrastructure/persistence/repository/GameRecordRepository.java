package kr.co.victoryfairy.diary.infrastructure.persistence.repository;

import kr.co.victoryfairy.diary.domain.DiaryEnum;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.GameRecordEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface GameRecordRepository extends JpaRepository<GameRecordEntity, Long> {

    List<GameRecordEntity> findByMemberAndSeason(MemberEntity member, String season);

    @EntityGraph(attributePaths = { "gameMatchEntity", "gameMatchEntity.homeTeamEntity", "stadiumEntity",
            "teamEntity" })
    List<GameRecordEntity> findByMemberIdAndSeasonOrderByGameMatchEntityMatchAtAsc(Long memberId, String season);

    List<GameRecordEntity> findByMemberId(Long memberId);

    GameRecordEntity findByMemberAndDiaryEntityId(MemberEntity member, Long diaryId);

    Optional<GameRecordEntity> findByDiaryEntityId(Long diaryId);

    // 리그 타입별 조회 메서드
    List<GameRecordEntity> findByMemberAndSeasonAndLeagueType(MemberEntity member, String season,
            MatchEnum.LeagueType leagueType);

    List<GameRecordEntity> findByMemberAndLeagueType(MemberEntity member, MatchEnum.LeagueType leagueType);

}
