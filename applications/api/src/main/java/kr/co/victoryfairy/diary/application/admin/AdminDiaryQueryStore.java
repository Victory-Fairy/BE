package kr.co.victoryfairy.diary.application.admin;

import kr.co.victoryfairy.diary.domain.DiaryModel;
import kr.co.victoryfairy.shared.domain.PageResult;

public interface AdminDiaryQueryStore {
    PageResult<DiaryModel.DiaryListResponse> findAll(DiaryModel.DiaryListRequest request);
}
