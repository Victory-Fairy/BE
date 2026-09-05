package kr.co.victoryfairy.media.infrastructure.persistence;

import kr.co.victoryfairy.media.domain.FileReference;
import kr.co.victoryfairy.media.domain.MediaFile;
import kr.co.victoryfairy.media.infrastructure.persistence.entity.FileEntity;
import kr.co.victoryfairy.media.infrastructure.persistence.entity.FileRefEntity;

public final class MediaPersistenceMapper {

    private MediaPersistenceMapper() {
    }

    public static MediaFile toDomain(FileEntity entity) {
        return new MediaFile(entity.getId(), entity.getName(), entity.getSaveName(), entity.getPath(), entity.getExt(),
                entity.getSize(), entity.getIsUse(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static FileEntity toEntity(MediaFile file) {
        return FileEntity.builder().name(file.name()).saveName(file.saveName()).path(file.path())
            .ext(file.ext()).size(file.size()).build();
    }

    public static FileReference toDomain(FileRefEntity entity) {
        return new FileReference(entity.getId(), toDomain(entity.getFileEntity()), entity.getRefId(),
                entity.getRefType(), entity.getIsUse(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

}
