package kr.co.victoryfairy.member.infrastructure.persistence.repository;

import kr.co.victoryfairy.member.domain.MemberModel;
import kr.co.victoryfairy.shared.domain.PageResult;

import java.util.Optional;

public interface MemberCustomRepository {

    Optional<MemberModel.MemberInfo> findById(Long memberId);

    PageResult<MemberModel.MemberListResponse> findAll(MemberModel.MemberListRequest request);

}
