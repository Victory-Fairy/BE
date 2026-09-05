package kr.co.victoryfairy.media.application;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.shared.application.model.CommonDto;
import kr.co.victoryfairy.media.domain.FileReference;
import kr.co.victoryfairy.media.domain.FileReferenceRepository;
import kr.co.victoryfairy.media.domain.MediaFileRepository;
import kr.co.victoryfairy.media.infrastructure.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** FileRef 저장과 조회 응답용 URL 생성을 조율합니다. */
@Service
@RequiredArgsConstructor
public class FileReferenceService {

    private final MediaFileRepository fileRepository;

    private final FileReferenceRepository fileRefRepository;

    private final S3PresignedUrlService s3PresignedUrlService;

    /**
     * 파일 참조 저장
     * @param refType 참조 타입
     * @param refId 참조 ID
     * @param fileIds 파일 ID 목록
     */
    @Transactional
    public void saveFileRefs(RefType refType, Long refId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        var files = fileRepository.findAllById(fileIds);
        var references = files.stream()
            .map(file -> FileReference.active(file, refId, refType))
            .toList();
        fileRefRepository.saveReferences(references);
    }

    /**
     * 기존 파일 참조 삭제 후 새로 저장
     * @param refType 참조 타입
     * @param refId 참조 ID
     * @param fileIds 새로운 파일 ID 목록
     */
    @Transactional
    public void replaceFileRefs(RefType refType, Long refId, List<Long> fileIds) {
        deleteFileRefs(refType, refId);
        saveFileRefs(refType, refId, fileIds);
    }

    /**
     * 파일 참조 삭제
     * @param refType 참조 타입
     * @param refId 참조 ID
     */
    @Transactional
    public void deleteFileRefs(RefType refType, Long refId) {
        var existingFileRefs = fileRefRepository.findActive(refType, refId);
        if (!existingFileRefs.isEmpty()) {
            fileRefRepository.deleteAll(existingFileRefs.stream().map(FileReference::id).toList());
        }
    }

    /**
     * 파일 참조 목록 조회
     * @param refType 참조 타입
     * @param refId 참조 ID
     * @return 이미지 DTO 목록
     */
    public List<CommonDto.ImageDto> findImagesByRefId(RefType refType, Long refId) {
        return fileRefRepository.findActive(refType, refId).stream().map(ref -> {
            var file = ref.file();
            return new CommonDto.ImageDto(file.id(), file.path(), file.saveName(), file.ext(),
                    s3PresignedUrlService.create(file.path(), file.saveName(), file.ext()));
        }).toList();
    }

    /**
     * 여러 참조 ID에 대한 이미지 맵 조회
     * @param refType 참조 타입
     * @param refIds 참조 ID 목록
     * @return refId -> ImageDto 맵 (첫 번째 파일만)
     */
    public Map<Long, CommonDto.ImageDto> findImageMapByRefIds(RefType refType, List<Long> refIds) {
        if (refIds == null || refIds.isEmpty()) {
            return Map.of();
        }

        return fileRefRepository.findActive(refType, refIds)
            .stream()
            .collect(Collectors.toMap(FileReference::refId, ref -> {
                var file = ref.file();
                return new CommonDto.ImageDto(file.id(), file.path(), file.saveName(), file.ext(),
                        s3PresignedUrlService.create(file.path(), file.saveName(), file.ext()));
            }, (existing, replacement) -> existing));
    }

}
