package kr.co.victoryfairy.media.domain;

import java.time.LocalDateTime;

public record MediaFile(Long id, String name, String saveName, String path, String ext, Long size, Boolean isUse,
        LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static MediaFile newFile(String name, String saveName, String path, String ext, Long size) {
        return new MediaFile(null, name, saveName, path, ext, size, true, null, null);
    }

}
