package kr.co.victoryfairy.core.api.service;

import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.api.domain.MatchDomain;

import java.time.LocalDate;
import java.util.List;

public interface MatchService {

    MatchDomain.MatchListResponse findList(LocalDate date);

    MatchDomain.MatchListResponse findList(LocalDate date, MatchEnum.LeagueType league);

    MatchDomain.MatchInfoResponse findById(String id);

    MatchDomain.RecordResponse findRecordById(String id);

    List<MatchDomain.InterestTeamMatchInfoResponse> findByTeam();

    MatchDomain.TodayMatchListResponse findTodayMatch();

}
