package kr.co.victoryfairy.member.infrastructure.persistence.repository;

import kr.co.victoryfairy.member.domain.MemberEnum;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface MemberInfoRepository extends JpaRepository<MemberInfoEntity, Long> {

    Optional<MemberInfoEntity> findBySnsTypeAndSnsId(MemberEnum.SnsType snsType, String snsId);

    Optional<MemberInfoEntity> findByNickNm(String nickNm);

    Optional<MemberInfoEntity> findByMemberEntity(MemberEntity memberEntity);

    Optional<MemberInfoEntity> findByMemberEntity_Id(Long memberId);

    List<MemberInfoEntity> findByMemberEntity_IdIn(List<Long> memberIds);

}
