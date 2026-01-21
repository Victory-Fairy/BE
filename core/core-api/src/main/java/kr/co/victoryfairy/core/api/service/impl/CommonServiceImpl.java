package kr.co.victoryfairy.core.api.service.impl;

import kr.co.victoryfairy.core.api.domain.CommonDomain;
import kr.co.victoryfairy.core.api.service.CommonService;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.storage.db.core.entity.SeatEntity;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import kr.co.victoryfairy.storage.db.core.repository.SeatRepository;
import kr.co.victoryfairy.storage.db.core.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonServiceImpl implements CommonService {

    private final TeamRepository teamRepository;
    private final SeatRepository seatRepository;

    @Override
    public List<CommonDomain.TeamListResponse> findAll(MatchEnum.LeagueType league) {
        List<TeamEntity> teams = (league == null)
                ? teamRepository.findAllByOrderByOrderNo()
                : teamRepository.findByLeagueOrderByOrderNo(league);

        return teams.stream()
                .map(entity -> new CommonDomain.TeamListResponse(
                        entity.getId(),
                        entity.getName(),
                        entity.getLabel(),
                        entity.getLeague(),
                        entity.getCountryCode()
                ))
                .toList();
    }

    @Override
    public List<CommonDomain.SeatListResponse> findSeat(Long id, String season) {
        List<SeatEntity> seatEntities = seatRepository.findByStadiumEntityIdAndSeason(id, season);
        if (seatEntities.isEmpty()) {
            return new ArrayList<>();
        }

        return seatEntities.stream()
                .map(entity -> new CommonDomain.SeatListResponse(entity.getId(), entity.getName()))
                .toList();
    }
}
