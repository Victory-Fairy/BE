package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.DiaryEntity;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<DiaryEntity, Long> {

    DiaryEntity findByMemberAndGameMatchEntity(MemberEntity memberEntity, GameMatchEntity gameMatchEntity);

    List<DiaryEntity> findByGameMatchEntityAndIsRatedFalse(GameMatchEntity gameMatchEntity);

    Optional<DiaryEntity> findByMemberIdAndId(Long memberId, Long id);

    List<DiaryEntity> findByMemberId(Long memberId);

    Optional<DiaryEntity> findByMemberIdAndGameMatchEntityId(Long memberId, String gameMatchEntityId);

}
