package kr.co.victoryfairy.core.admin.service;

import kr.co.victoryfairy.core.admin.domain.DiaryDomain;
import kr.co.victoryfairy.support.model.PageResult;

public interface DiaryService {
    PageResult<DiaryDomain.DiaryListResponse> findAll(DiaryDomain.DiaryListRequest request);
}
