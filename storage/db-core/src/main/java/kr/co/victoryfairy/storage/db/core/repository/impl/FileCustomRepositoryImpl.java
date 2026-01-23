package kr.co.victoryfairy.storage.db.core.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.co.victoryfairy.storage.db.core.entity.FileEntity;
import kr.co.victoryfairy.storage.db.core.entity.QFileRefEntity;
import kr.co.victoryfairy.storage.db.core.repository.FileCustomRepository;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static kr.co.victoryfairy.storage.db.core.entity.QFileEntity.fileEntity;
import static kr.co.victoryfairy.storage.db.core.entity.QFileRefEntity.fileRefEntity;

@Service
public class FileCustomRepositoryImpl extends QuerydslRepositorySupport implements FileCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public FileCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        super(FileEntity.class);
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<FileEntity> findMissingFile(LocalDateTime date) {
        return jpaQueryFactory
            .select(Projections.fields(FileEntity.class, fileEntity.id, fileEntity.name, fileEntity.saveName,
                    fileEntity.path, fileEntity.ext, fileEntity.size))
            .from(fileEntity)
            .leftJoin(fileRefEntity)
            .on(fileEntity.id.eq(fileRefEntity.fileEntity.id))
            .where(fileRefEntity.fileEntity.id.isNull().and(fileEntity.createdAt.before(date)))
            .fetch();
    }

}
