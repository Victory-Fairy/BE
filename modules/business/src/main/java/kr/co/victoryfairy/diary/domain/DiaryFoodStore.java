package kr.co.victoryfairy.diary.domain;

import java.util.List;
import java.util.Map;
import kr.co.victoryfairy.shared.domain.RefType;

public interface DiaryFoodStore {
    void saveFoods(RefType refType, Long refId, List<String> names);
    void deleteFoods(RefType refType, Long refId);
    List<String> findNames(RefType refType, Long refId);
    Map<Long, List<String>> findNames(RefType refType, List<Long> refIds);
}
