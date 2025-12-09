package kr.co.victoryfairy.storage.db.core.repository;

import io.dodn.springboot.core.enums.MemberEnum;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberInfoRepository extends JpaRepository<MemberInfoEntity, Long> {

    Optional<MemberInfoEntity> findBySnsTypeAndSnsId(MemberEnum.SnsType snsType, String snsId);

    Optional<MemberInfoEntity> findByNickNm(String nickNm);

    Optional<MemberInfoEntity> findByMemberEntity(MemberEntity memberEntity);

    Optional<MemberInfoEntity> findByMemberEntity_Id(Long memberId);
}
