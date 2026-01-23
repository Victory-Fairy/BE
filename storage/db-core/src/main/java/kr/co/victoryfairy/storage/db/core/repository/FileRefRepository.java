package kr.co.victoryfairy.storage.db.core.repository;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.storage.db.core.entity.FileRefEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileRefRepository extends JpaRepository<FileRefEntity, Long> {

    Optional<FileRefEntity> findByFileEntityIdAndIsUseTrue(Long fileId);

    @EntityGraph(attributePaths = { "fileEntity" })
    List<FileRefEntity> findByRefTypeAndRefIdInAndIsUseTrue(RefType refType, List<Long> refIds);

    @EntityGraph(attributePaths = { "fileEntity" })
    List<FileRefEntity> findAllByRefTypeAndRefIdAndIsUseTrue(RefType refType, Long refId);

    Optional<FileRefEntity> findByRefTypeAndRefIdAndIsUseTrue(RefType refType, Long refId);

}
