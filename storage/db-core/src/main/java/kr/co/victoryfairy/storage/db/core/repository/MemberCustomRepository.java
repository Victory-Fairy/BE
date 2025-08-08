package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.model.MemberModel;
import kr.co.victoryfairy.support.model.PageResult;

import java.util.Optional;

public interface MemberCustomRepository {

    Optional<MemberModel.MemberInfo> findById(Long memberId);
    PageResult<MemberModel.MemberListResponse> findAll(MemberModel.MemberListRequest request);
}
