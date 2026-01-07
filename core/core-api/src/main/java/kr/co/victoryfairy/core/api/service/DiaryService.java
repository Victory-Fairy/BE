package kr.co.victoryfairy.core.api.service;


import kr.co.victoryfairy.core.api.domain.DiaryDomain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface DiaryService {

    DiaryDomain.WriteResponse writeDiary(Long memberId, DiaryDomain.WriteRequest diaryDto);

    void updateDiary(Long diaryId, DiaryDomain.UpdateRequest request);

    void deleteDiary(Long diaryId);

    List<DiaryDomain.ListResponse> findList(YearMonth date);

    List<DiaryDomain.DailyListResponse> findDailyList(LocalDate date);

    DiaryDomain.DiaryDetailResponse findById(Long id);
}
