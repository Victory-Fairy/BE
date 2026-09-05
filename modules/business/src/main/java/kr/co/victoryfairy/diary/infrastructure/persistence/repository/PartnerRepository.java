package kr.co.victoryfairy.diary.infrastructure.persistence.repository;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.PartnerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerRepository extends JpaRepository<PartnerEntity, Long> {

    List<PartnerEntity> findByRefTypeAndRefId(RefType refType, Long refId);

    List<PartnerEntity> findByRefTypeAndRefIdIn(RefType refType, List<Long> refIds);

}
