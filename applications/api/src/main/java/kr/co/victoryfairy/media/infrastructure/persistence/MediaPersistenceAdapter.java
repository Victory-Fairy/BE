package kr.co.victoryfairy.media.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import kr.co.victoryfairy.media.domain.FileReference;
import kr.co.victoryfairy.media.domain.FileReferenceRepository;
import kr.co.victoryfairy.media.domain.MediaFile;
import kr.co.victoryfairy.media.domain.MediaFileRepository;
import kr.co.victoryfairy.media.infrastructure.persistence.entity.FileRefEntity;
import kr.co.victoryfairy.media.infrastructure.persistence.repository.FileRefRepository;
import kr.co.victoryfairy.media.infrastructure.persistence.repository.FileRepository;
import kr.co.victoryfairy.shared.domain.RefType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MediaPersistenceAdapter implements MediaFileRepository, FileReferenceRepository {

    private final FileRepository files;
    private final FileRefRepository references;

    @Override
    public List<MediaFile> createAll(List<MediaFile> values) {
        return files.saveAll(values.stream().map(MediaPersistenceMapper::toEntity).toList()).stream()
            .map(MediaPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<MediaFile> findAllById(List<Long> ids) {
        return files.findAllById(ids).stream().map(MediaPersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<MediaFile> findById(Long id) {
        return files.findById(id).map(MediaPersistenceMapper::toDomain);
    }

    @Override
    public void saveReferences(List<FileReference> values) {
        references.saveAll(values.stream().map(value -> FileRefEntity.builder()
            .fileEntity(files.getReferenceById(value.file().id()))
            .refId(value.refId()).refType(value.refType()).build()).toList());
    }

    @Override
    public void save(FileReference value) {
        references.save(FileRefEntity.builder().fileEntity(files.getReferenceById(value.file().id()))
            .refId(value.refId()).refType(value.refType()).build());
    }

    @Override
    public List<FileReference> findActive(RefType refType, Long refId) {
        return references.findAllByRefTypeAndRefIdAndIsUseTrue(refType, refId).stream()
            .map(MediaPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<FileReference> findActive(RefType refType, List<Long> refIds) {
        return references.findByRefTypeAndRefIdInAndIsUseTrue(refType, refIds).stream()
            .map(MediaPersistenceMapper::toDomain).toList();
    }

    @Override
    public void deleteAll(List<Long> referenceIds) {
        references.deleteAllById(referenceIds);
    }

    @Override
    public void deactivateFirstActive(RefType refType, Long refId) {
        references.findByRefTypeAndRefIdAndIsUseTrue(refType, refId).ifPresent(FileRefEntity::delete);
    }

}
