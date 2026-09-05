package kr.co.victoryfairy.member.domain;

import java.util.Optional;
import kr.co.victoryfairy.shared.domain.PageResult;

public interface MemberQueryStore {

    Optional<MemberModel.MemberInfo> findById(Long memberId);

    PageResult<MemberModel.MemberListResponse> findAll(MemberModel.MemberListRequest request);
}
