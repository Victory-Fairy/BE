package kr.co.victoryfairy.storage.db.core.repository;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.storage.db.core.entity.PartnerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerRepository extends JpaRepository<PartnerEntity, Long> {

    List<PartnerEntity> findByRefTypeAndRefId(RefType refType, Long refId);

    List<PartnerEntity> findByRefTypeAndRefIdIn(RefType refType, List<Long> refIds);

}
