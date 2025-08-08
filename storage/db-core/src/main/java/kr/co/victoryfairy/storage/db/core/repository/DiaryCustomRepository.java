package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.model.DiaryModel;
import kr.co.victoryfairy.support.model.PageResult;

import java.time.LocalDate;
import java.util.List;

public interface DiaryCustomRepository {

    List<DiaryModel.DiaryDto> findList(DiaryModel.ListRequest request);
    List<DiaryModel.DiaryDto> findDailyList(DiaryModel.DailyListRequest request);

    PageResult<DiaryModel.DiaryListResponse> findAll(DiaryModel.DiaryListRequest request);
}
