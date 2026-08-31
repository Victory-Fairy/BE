package kr.co.victoryfairy.core.api.service;

import kr.co.victoryfairy.core.api.domain.CommonDomain;
import io.dodn.springboot.core.enums.MatchEnum;

import java.util.List;

public interface CommonService {

    List<CommonDomain.TeamListResponse> findAll(MatchEnum.LeagueType league);

    List<CommonDomain.SeatListResponse> findSeat(Long id, String season);

}
