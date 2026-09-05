package kr.co.victoryfairy.diary.infrastructure.persistence.repository;

import kr.co.victoryfairy.diary.domain.DiaryModel;
import kr.co.victoryfairy.diary.domain.DiaryQueryStore;

import java.time.LocalDate;
import java.util.List;

public interface DiaryCustomRepository extends DiaryQueryStore {

    List<DiaryModel.DiaryDto> findList(DiaryModel.ListRequest request);

    List<DiaryModel.DiaryDto> findDailyList(DiaryModel.DailyListRequest request);

}
