package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.DiaryEntity;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import io.dodn.springboot.core.enums.MatchEnum;

@Repository
public interface DiaryRepository extends JpaRepository<DiaryEntity, Long> {

    DiaryEntity findByMemberAndGameMatchEntity(MemberEntity memberEntity, GameMatchEntity gameMatchEntity);

    List<DiaryEntity> findByGameMatchEntityAndIsRatedFalse(GameMatchEntity gameMatchEntity);

    List<DiaryEntity> findByIsRatedFalseAndGameMatchEntityStatusIn(List<MatchEnum.MatchStatus> statuses);

    Optional<DiaryEntity> findByMemberIdAndId(Long memberId, Long id);

    List<DiaryEntity> findByMemberId(Long memberId);

    Optional<DiaryEntity> findByMemberIdAndGameMatchEntityId(Long memberId, String gameMatchEntityId);

}
