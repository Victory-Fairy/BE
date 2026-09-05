package kr.co.victoryfairy.media.domain;

import java.util.List;
import java.util.Optional;

public interface MediaFileRepository {

    List<MediaFile> createAll(List<MediaFile> files);

    List<MediaFile> findAllById(List<Long> ids);

    Optional<MediaFile> findById(Long id);

}
