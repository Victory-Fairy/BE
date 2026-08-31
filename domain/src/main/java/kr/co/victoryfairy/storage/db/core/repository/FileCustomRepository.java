package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.FileEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface FileCustomRepository {

    List<FileEntity> findMissingFile(LocalDateTime date);

}
