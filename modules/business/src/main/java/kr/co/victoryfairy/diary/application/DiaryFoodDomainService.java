package kr.co.victoryfairy.diary.application;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.diary.domain.DiaryFoodStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 음식 도메인 서비스
 * <p>
 * 일기에 기록된 음식 관련 공통 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class DiaryFoodDomainService {

    private final DiaryFoodStore diaryFoodStore;

    /**
     * 음식 목록 저장
     * @param refType 참조 타입
     * @param refId 참조 ID
     * @param foodNameList 음식 이름 목록
     */
    @Transactional
    public void saveFoods(RefType refType, Long refId, List<String> foodNameList) {
        if (foodNameList == null || foodNameList.isEmpty()) {
            return;
        }

        diaryFoodStore.saveFoods(refType, refId, foodNameList);
    }

    /**
     * 기존 음식 목록 삭제 후 새로 저장
     * @param refType 참조 타입
     * @param refId 참조 ID
     * @param foodNameList 새로운 음식 이름 목록
     */
    @Transactional
    public void replaceFoods(RefType refType, Long refId, List<String> foodNameList) {
        deleteFoods(refType, refId);
        saveFoods(refType, refId, foodNameList);
    }

    /**
     * 음식 목록 삭제
     * @param refType 참조 타입
     * @param refId 참조 ID
     */
    @Transactional
    public void deleteFoods(RefType refType, Long refId) {
        diaryFoodStore.deleteFoods(refType, refId);
    }

    /**
     * 음식 이름 목록 조회
     * @param refType 참조 타입
     * @param refId 참조 ID
     * @return 음식 이름 목록
     */
    public List<String> findFoodNamesByRefId(RefType refType, Long refId) {
        return diaryFoodStore.findNames(refType, refId);
    }

    /**
     * 여러 참조 ID에 대한 음식 맵 조회
     * @param refType 참조 타입
     * @param refIds 참조 ID 목록
     * @return refId -> 음식 이름 목록 맵
     */
    public Map<Long, List<String>> findFoodMapByRefIds(RefType refType, List<Long> refIds) {
        if (refIds == null || refIds.isEmpty()) {
            return Map.of();
        }

        return diaryFoodStore.findNames(refType, refIds);
    }

}
