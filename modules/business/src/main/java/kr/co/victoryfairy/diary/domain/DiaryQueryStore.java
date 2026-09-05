package kr.co.victoryfairy.diary.domain;

import java.util.List;
import kr.co.victoryfairy.shared.domain.PageResult;

public interface DiaryQueryStore {
    List<DiaryModel.DiaryDto> findList(DiaryModel.ListRequest request);
    List<DiaryModel.DiaryDto> findDailyList(DiaryModel.DailyListRequest request);
    PageResult<DiaryModel.DiaryListResponse> findAll(DiaryModel.DiaryListRequest request);
}
