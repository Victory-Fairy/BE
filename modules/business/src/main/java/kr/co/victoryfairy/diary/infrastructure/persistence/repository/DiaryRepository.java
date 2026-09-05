package kr.co.victoryfairy.diary.infrastructure.persistence.repository;

import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import kr.co.victoryfairy.game.domain.MatchEnum;

@Repository
public interface DiaryRepository extends JpaRepository<DiaryEntity, Long> {

    DiaryEntity findByMemberAndGameMatchEntity(MemberEntity memberEntity, GameMatchEntity gameMatchEntity);

    List<DiaryEntity> findByGameMatchEntityAndIsRatedFalse(GameMatchEntity gameMatchEntity);

    List<DiaryEntity> findByIsRatedFalseAndGameMatchEntityStatusIn(List<MatchEnum.MatchStatus> statuses);

    Optional<DiaryEntity> findByMemberIdAndId(Long memberId, Long id);

    @EntityGraph(attributePaths = { "gameMatchEntity", "gameMatchEntity.homeTeamEntity", "teamEntity" })
    Optional<DiaryEntity> findDetailByMemberIdAndId(Long memberId, Long id);

    List<DiaryEntity> findByMemberId(Long memberId);

    Optional<DiaryEntity> findByMemberIdAndGameMatchEntityId(Long memberId, String gameMatchEntityId);

    List<DiaryEntity> findByMemberIdAndGameMatchEntityIdIn(Long memberId, Collection<String> gameMatchEntityIds);

}
