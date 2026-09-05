package kr.co.victoryfairy.media.domain;

import java.time.LocalDateTime;

import kr.co.victoryfairy.shared.domain.RefType;

public record FileReference(Long id, MediaFile file, Long refId, RefType refType, Boolean isUse,
        LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static FileReference active(MediaFile file, Long refId, RefType refType) {
        return new FileReference(null, file, refId, refType, true, null, null);
    }

}
