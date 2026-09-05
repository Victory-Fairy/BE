package kr.co.victoryfairy.diary.application;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.diary.domain.PartnerStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 동행자(파트너) 도메인 서비스
 * <p>
 * 일기에 기록된 동행자 관련 공통 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class PartnerDomainService {

    private final PartnerStore partnerStore;

    /**
     * 동행자 목록 저장
     * @param refType 참조 타입
     * @param refId 참조 ID
     * @param partnerList 동행자 정보 목록
     */
    @Transactional
    public void savePartners(RefType refType, Long refId, List<PartnerDto.PartnerSaveRequest> partnerList) {
        if (partnerList == null || partnerList.isEmpty()) {
            return;
        }

        partnerStore.savePartners(refType, refId, partnerList.stream()
            .map(value -> new PartnerStore.Partner(value.name(), value.teamId())).toList());
    }

    /**
     * 기존 동행자 목록 삭제 후 새로 저장
     * @param refType 참조 타입
     * @param refId 참조 ID
     * @param partnerList 새로운 동행자 정보 목록
     */
    @Transactional
    public void replacePartners(RefType refType, Long refId, List<PartnerDto.PartnerSaveRequest> partnerList) {
        deletePartners(refType, refId);
        savePartners(refType, refId, partnerList);
    }

    /**
     * 동행자 목록 삭제
     * @param refType 참조 타입
     * @param refId 참조 ID
     */
    @Transactional
    public void deletePartners(RefType refType, Long refId) {
        partnerStore.deletePartners(refType, refId);
    }

    /**
     * 동행자 목록 조회
     * @param refType 참조 타입
     * @param refId 참조 ID
     * @return 동행자 응답 목록
     */
    public List<PartnerDto.PartnerResponse> findPartnersByRefId(RefType refType, Long refId) {
        return partnerStore.find(refType, refId).stream()
            .map(value -> new PartnerDto.PartnerResponse(value.name(), value.teamId())).toList();
    }

    /**
     * 여러 참조 ID에 대한 동행자 이름 맵 조회
     * @param refType 참조 타입
     * @param refIds 참조 ID 목록
     * @return refId -> 동행자 이름 목록 맵
     */
    public Map<Long, List<String>> findPartnerNameMapByRefIds(RefType refType, List<Long> refIds) {
        if (refIds == null || refIds.isEmpty()) {
            return Map.of();
        }

        return partnerStore.findNameMap(refType, refIds);
    }

}
