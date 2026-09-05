package kr.co.victoryfairy.diary.domain;

import java.util.List;

public interface DiaryQueryStore {
    List<DiaryModel.DiaryDto> findList(DiaryModel.ListRequest request);
    List<DiaryModel.DiaryDto> findDailyList(DiaryModel.DailyListRequest request);
}
