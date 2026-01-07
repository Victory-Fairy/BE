package kr.co.victoryfairy.common.service;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.common.model.CommonDto;
import kr.co.victoryfairy.storage.db.core.entity.PartnerEntity;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import kr.co.victoryfairy.storage.db.core.repository.PartnerRepository;
import kr.co.victoryfairy.storage.db.core.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 동행자(파트너) 도메인 서비스
 * <p>
 * 일기에 기록된 동행자 관련 공통 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class PartnerDomainService {

    private final PartnerRepository partnerRepository;
    private final TeamRepository teamRepository;

    /**
     * 동행자 목록 저장
     *
     * @param refType     참조 타입 (DIARY, FREE_DIARY)
     * @param refId       참조 ID
     * @param partnerList 동행자 정보 목록
     */
    @Transactional
    public void savePartners(RefType refType, Long refId, List<CommonDto.PartnerSaveRequest> partnerList) {
        if (partnerList == null || partnerList.isEmpty()) {
            return;
        }

        List<PartnerEntity> partnerEntityList = new ArrayList<>();
        for (CommonDto.PartnerSaveRequest partnerDto : partnerList) {
            TeamEntity partnerTeamEntity = null;
            String partnerTeamName = null;

            if (partnerDto.teamId() != null) {
                partnerTeamEntity = teamRepository.findById(partnerDto.teamId()).orElse(null);
                if (partnerTeamEntity != null) {
                    partnerTeamName = partnerTeamEntity.getName();
                }
            }

            PartnerEntity partnerEntity = PartnerEntity.builder()
                    .refId(refId)
                    .refType(refType)
                    .name(partnerDto.name())
                    .teamName(partnerTeamName)
                    .teamEntity(partnerTeamEntity)
                    .build();
            partnerEntityList.add(partnerEntity);
        }
        partnerRepository.saveAll(partnerEntityList);
    }

    /**
     * 기존 동행자 목록 삭제 후 새로 저장
     *
     * @param refType     참조 타입
     * @param refId       참조 ID
     * @param partnerList 새로운 동행자 정보 목록
     */
    @Transactional
    public void replacePartners(RefType refType, Long refId, List<CommonDto.PartnerSaveRequest> partnerList) {
        deletePartners(refType, refId);
        savePartners(refType, refId, partnerList);
    }

    /**
     * 동행자 목록 삭제
     *
     * @param refType 참조 타입
     * @param refId   참조 ID
     */
    @Transactional
    public void deletePartners(RefType refType, Long refId) {
        var existingPartners = partnerRepository.findByRefTypeAndRefId(refType, refId);
        if (!existingPartners.isEmpty()) {
            partnerRepository.deleteAll(existingPartners);
        }
    }

    /**
     * 동행자 목록 조회
     *
     * @param refType 참조 타입
     * @param refId   참조 ID
     * @return 동행자 응답 목록
     */
    public List<CommonDto.PartnerResponse> findPartnersByRefId(RefType refType, Long refId) {
        return partnerRepository.findByRefTypeAndRefId(refType, refId).stream()
                .map(entity -> new CommonDto.PartnerResponse(
                        entity.getName(),
                        entity.getTeamEntity() != null ? entity.getTeamEntity().getId() : null
                ))
                .toList();
    }

    /**
     * 여러 참조 ID에 대한 동행자 이름 맵 조회
     *
     * @param refType 참조 타입
     * @param refIds  참조 ID 목록
     * @return refId -> 동행자 이름 목록 맵
     */
    public Map<Long, List<String>> findPartnerNameMapByRefIds(RefType refType, List<Long> refIds) {
        if (refIds == null || refIds.isEmpty()) {
            return Map.of();
        }

        return partnerRepository.findByRefTypeAndRefIdIn(refType, refIds).stream()
                .collect(Collectors.groupingBy(
                        PartnerEntity::getRefId,
                        Collectors.mapping(PartnerEntity::getName, Collectors.toList())
                ));
    }
}