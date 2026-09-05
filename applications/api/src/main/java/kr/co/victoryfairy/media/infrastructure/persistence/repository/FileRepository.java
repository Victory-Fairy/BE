package kr.co.victoryfairy.media.infrastructure.persistence.repository;

import kr.co.victoryfairy.media.infrastructure.persistence.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

}
