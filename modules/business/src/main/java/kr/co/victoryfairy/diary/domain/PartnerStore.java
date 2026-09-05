package kr.co.victoryfairy.diary.domain;

import java.util.List;
import java.util.Map;
import kr.co.victoryfairy.shared.domain.RefType;

public interface PartnerStore {
    record Partner(String name, Long teamId) {}
    void savePartners(RefType refType, Long refId, List<Partner> partners);
    void deletePartners(RefType refType, Long refId);
    List<Partner> find(RefType refType, Long refId);
    Map<Long, List<String>> findNameMap(RefType refType, List<Long> refIds);
}
