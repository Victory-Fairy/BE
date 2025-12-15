package kr.co.victoryfairy.storage.db.core.repository;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.storage.db.core.entity.DiaryFoodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaryFoodRepository extends JpaRepository<DiaryFoodEntity,Long> {
    List<DiaryFoodEntity> findByRefTypeAndRefId(RefType refType, Long refId);
    List<DiaryFoodEntity> findByRefTypeAndRefIdIn(RefType refType, List<Long> refIds);
}
