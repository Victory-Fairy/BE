package kr.co.victoryfairy.media.domain;

import java.util.List;

import kr.co.victoryfairy.shared.domain.RefType;

public interface FileReferenceRepository {

    void saveReferences(List<FileReference> references);

    void save(FileReference reference);

    List<FileReference> findActive(RefType refType, Long refId);

    List<FileReference> findActive(RefType refType, List<Long> refIds);

    void deleteAll(List<Long> referenceIds);

    void deactivateFirstActive(RefType refType, Long refId);

}
