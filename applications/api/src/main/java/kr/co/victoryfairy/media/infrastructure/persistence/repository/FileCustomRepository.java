package kr.co.victoryfairy.media.infrastructure.persistence.repository;

import kr.co.victoryfairy.media.infrastructure.persistence.entity.FileEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface FileCustomRepository {

    List<FileEntity> findMissingFile(LocalDateTime date);

}
