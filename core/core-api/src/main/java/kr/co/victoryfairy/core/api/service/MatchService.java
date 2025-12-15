package kr.co.victoryfairy.core.api.service;

import kr.co.victoryfairy.core.api.domain.MatchDomain;

import java.time.LocalDate;
import java.util.List;

public interface MatchService {
    MatchDomain.MatchListResponse findList(LocalDate date);

    MatchDomain.MatchInfoResponse findById(String id);

    MatchDomain.RecordResponse findRecordById(String id);

    List<MatchDomain.InterestTeamMatchInfoResponse> findByTeam();

    MatchDomain.TodayMatchListResponse findTodayMatch();
}
