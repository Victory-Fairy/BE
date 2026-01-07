package kr.co.victoryfairy.common.service;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.common.model.CommonDto;
import kr.co.victoryfairy.storage.db.core.entity.FileRefEntity;
import kr.co.victoryfairy.storage.db.core.repository.FileRefRepository;
import kr.co.victoryfairy.storage.db.core.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 파일 참조 도메인 서비스
 * <p>
 * FileRef 관련 공통 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class FileRefDomainService {

    private final FileRepository fileRepository;
    private final FileRefRepository fileRefRepository;

    /**
     * 파일 참조 저장
     *
     * @param refType 참조 타입 (DIARY, FREE_DIARY, PROFILE 등)
     * @param refId   참조 ID
     * @param fileIds 파일 ID 목록
     */
    @Transactional
    public void saveFileRefs(RefType refType, Long refId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        var fileEntities = fileRepository.findAllById(fileIds);
        var fileRefEntities = fileEntities.stream()
                .map(file -> FileRefEntity.builder()
                        .fileEntity(file)
                        .refId(refId)
                        .refType(refType)
                        .build()
                )
                .toList();
        fileRefRepository.saveAll(fileRefEntities);
    }

    /**
     * 기존 파일 참조 삭제 후 새로 저장
     *
     * @param refType 참조 타입
     * @param refId   참조 ID
     * @param fileIds 새로운 파일 ID 목록
     */
    @Transactional
    public void replaceFileRefs(RefType refType, Long refId, List<Long> fileIds) {
        deleteFileRefs(refType, refId);
        saveFileRefs(refType, refId, fileIds);
    }

    /**
     * 파일 참조 삭제
     *
     * @param refType 참조 타입
     * @param refId   참조 ID
     */
    @Transactional
    public void deleteFileRefs(RefType refType, Long refId) {
        var existingFileRefs = fileRefRepository.findAllByRefTypeAndRefIdAndIsUseTrue(refType, refId);
        if (!existingFileRefs.isEmpty()) {
            fileRefRepository.deleteAll(existingFileRefs);
        }
    }

    /**
     * 파일 참조 목록 조회
     *
     * @param refType 참조 타입
     * @param refId   참조 ID
     * @return 이미지 DTO 목록
     */
    public List<CommonDto.ImageDto> findImagesByRefId(RefType refType, Long refId) {
        return fileRefRepository.findAllByRefTypeAndRefIdAndIsUseTrue(refType, refId).stream()
                .map(ref -> {
                    var file = ref.getFileEntity();
                    return new CommonDto.ImageDto(file.getId(), file.getPath(), file.getSaveName(), file.getExt());
                })
                .toList();
    }

    /**
     * 여러 참조 ID에 대한 이미지 맵 조회
     *
     * @param refType 참조 타입
     * @param refIds  참조 ID 목록
     * @return refId -> ImageDto 맵 (첫 번째 파일만)
     */
    public Map<Long, CommonDto.ImageDto> findImageMapByRefIds(RefType refType, List<Long> refIds) {
        if (refIds == null || refIds.isEmpty()) {
            return Map.of();
        }

        return fileRefRepository.findByRefTypeAndRefIdInAndIsUseTrue(refType, refIds).stream()
                .collect(Collectors.toMap(
                        FileRefEntity::getRefId,
                        ref -> {
                            var file = ref.getFileEntity();
                            return new CommonDto.ImageDto(file.getId(), file.getPath(), file.getSaveName(), file.getExt());
                        },
                        (existing, replacement) -> existing
                ));
    }
}