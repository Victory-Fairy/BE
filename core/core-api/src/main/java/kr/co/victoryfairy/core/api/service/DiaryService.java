package kr.co.victoryfairy.core.api.service;


import kr.co.victoryfairy.core.api.domain.DiaryDomain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface DiaryService {

    void writeDiary(DiaryDomain.WriteRequest diaryDto);

    List<DiaryDomain.ListResponse> findList(YearMonth date);

    List<DiaryDomain.DailyListResponse> findDailyList(LocalDate date);
}
