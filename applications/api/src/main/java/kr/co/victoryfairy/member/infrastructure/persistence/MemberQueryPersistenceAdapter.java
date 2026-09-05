package kr.co.victoryfairy.member.infrastructure.persistence;

import java.util.Optional;
import kr.co.victoryfairy.member.domain.MemberModel;
import kr.co.victoryfairy.member.domain.MemberQueryStore;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberCustomRepository;
import kr.co.victoryfairy.shared.domain.PageResult;
import org.springframework.stereotype.Repository;

@Repository
public class MemberQueryPersistenceAdapter implements MemberQueryStore {

    private final MemberCustomRepository repository;

    public MemberQueryPersistenceAdapter(MemberCustomRepository repository) {
        this.repository = repository;
    }

    public Optional<MemberModel.MemberInfo> findById(Long memberId) {
        return repository.findById(memberId);
    }

    public PageResult<MemberModel.MemberListResponse> findAll(MemberModel.MemberListRequest request) {
        return repository.findAll(request);
    }
}
