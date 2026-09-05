package kr.co.victoryfairy.common.application;

import kr.co.victoryfairy.common.presentation.CommonDomain;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.SeatEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.SeatRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonQueryService {

    private final TeamRepository teamRepository;

    private final SeatRepository seatRepository;

    public List<CommonDomain.TeamListResponse> findAll(MatchEnum.LeagueType league) {
        List<TeamEntity> teams = (league == null) ? teamRepository.findAllByOrderByOrderNo()
                : teamRepository.findByLeagueOrderByOrderNo(league);

        return teams.stream()
            .map(entity -> new CommonDomain.TeamListResponse(entity.getId(), entity.getName(), entity.getLabel(),
                    entity.getLeague(), entity.getCountryCode()))
            .toList();
    }

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
