package kr.co.victoryfairy.core.api.service;

import kr.co.victoryfairy.core.api.domain.FreeDiaryDomain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface FreeDiaryService {

    FreeDiaryDomain.WriteResponse write(FreeDiaryDomain.WriteRequest request);

    void update(Long id, FreeDiaryDomain.UpdateRequest request);

    void delete(Long id);

    FreeDiaryDomain.DetailResponse findById(Long id);

    List<FreeDiaryDomain.ListResponse> findList(YearMonth date);

    List<FreeDiaryDomain.DailyListResponse> findDailyList(LocalDate date);
}