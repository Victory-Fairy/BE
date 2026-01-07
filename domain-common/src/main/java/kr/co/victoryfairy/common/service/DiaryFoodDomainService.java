package kr.co.victoryfairy.common.service;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.storage.db.core.entity.DiaryFoodEntity;
import kr.co.victoryfairy.storage.db.core.repository.DiaryFoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 음식 도메인 서비스
 * <p>
 * 일기에 기록된 음식 관련 공통 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class DiaryFoodDomainService {

    private final DiaryFoodRepository diaryFoodRepository;

    /**
     * 음식 목록 저장
     *
     * @param refType       참조 타입 (DIARY, FREE_DIARY)
     * @param refId         참조 ID
     * @param foodNameList  음식 이름 목록
     */
    @Transactional
    public void saveFoods(RefType refType, Long refId, List<String> foodNameList) {
        if (foodNameList == null || foodNameList.isEmpty()) {
            return;
        }

        var foodEntities = foodNameList.stream()
                .map(food -> DiaryFoodEntity.builder()
                        .refId(refId)
                        .refType(refType)
                        .foodName(food)
                        .build()
                )
                .toList();
        diaryFoodRepository.saveAll(foodEntities);
    }

    /**
     * 기존 음식 목록 삭제 후 새로 저장
     *
     * @param refType       참조 타입
     * @param refId         참조 ID
     * @param foodNameList  새로운 음식 이름 목록
     */
    @Transactional
    public void replaceFoods(RefType refType, Long refId, List<String> foodNameList) {
        deleteFoods(refType, refId);
        saveFoods(refType, refId, foodNameList);
    }

    /**
     * 음식 목록 삭제
     *
     * @param refType 참조 타입
     * @param refId   참조 ID
     */
    @Transactional
    public void deleteFoods(RefType refType, Long refId) {
        var existingFoods = diaryFoodRepository.findByRefTypeAndRefId(refType, refId);
        if (!existingFoods.isEmpty()) {
            diaryFoodRepository.deleteAll(existingFoods);
        }
    }

    /**
     * 음식 이름 목록 조회
     *
     * @param refType 참조 타입
     * @param refId   참조 ID
     * @return 음식 이름 목록
     */
    public List<String> findFoodNamesByRefId(RefType refType, Long refId) {
        return diaryFoodRepository.findByRefTypeAndRefId(refType, refId).stream()
                .map(DiaryFoodEntity::getFoodName)
                .toList();
    }

    /**
     * 여러 참조 ID에 대한 음식 맵 조회
     *
     * @param refType 참조 타입
     * @param refIds  참조 ID 목록
     * @return refId -> 음식 이름 목록 맵
     */
    public Map<Long, List<String>> findFoodMapByRefIds(RefType refType, List<Long> refIds) {
        if (refIds == null || refIds.isEmpty()) {
            return Map.of();
        }

        return diaryFoodRepository.findByRefTypeAndRefIdIn(refType, refIds).stream()
                .collect(Collectors.groupingBy(
                        DiaryFoodEntity::getRefId,
                        Collectors.mapping(DiaryFoodEntity::getFoodName, Collectors.toList())
                ));
    }
}