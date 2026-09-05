package kr.co.victoryfairy.member.infrastructure.persistence.repository;

import kr.co.victoryfairy.member.infrastructure.persistence.model.MemberModel;
import kr.co.victoryfairy.shared.infrastructure.persistence.model.PageResult;

import java.util.Optional;

public interface MemberCustomRepository {

    Optional<MemberModel.MemberInfo> findById(Long memberId);

    PageResult<MemberModel.MemberListResponse> findAll(MemberModel.MemberListRequest request);

}
