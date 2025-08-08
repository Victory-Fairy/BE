package kr.co.victoryfairy.core.admin.service;

import kr.co.victoryfairy.core.admin.domain.MemberDomain;
import kr.co.victoryfairy.support.model.PageResult;

import java.util.List;

public interface MemberService {
    PageResult<MemberDomain.MemberListResponse> findList(MemberDomain.MemberListRequest request);
}
