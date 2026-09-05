package kr.co.victoryfairy.diary.infrastructure.persistence.repository;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryFoodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaryFoodRepository extends JpaRepository<DiaryFoodEntity, Long> {

    List<DiaryFoodEntity> findByRefTypeAndRefId(RefType refType, Long refId);

    List<DiaryFoodEntity> findByRefTypeAndRefIdIn(RefType refType, List<Long> refIds);

}
