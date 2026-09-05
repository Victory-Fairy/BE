package kr.co.victoryfairy.diary.infrastructure.persistence.repository;

import kr.co.victoryfairy.diary.infrastructure.persistence.model.DiaryModel;
import kr.co.victoryfairy.shared.domain.PageResult;

import java.time.LocalDate;
import java.util.List;

public interface DiaryCustomRepository {

    List<DiaryModel.DiaryDto> findList(DiaryModel.ListRequest request);

    List<DiaryModel.DiaryDto> findDailyList(DiaryModel.DailyListRequest request);

    PageResult<DiaryModel.DiaryListResponse> findAll(DiaryModel.DiaryListRequest request);

}
