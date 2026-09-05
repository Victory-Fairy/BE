package kr.co.victoryfairy.common.application;

import kr.co.victoryfairy.common.presentation.CommonDomain;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.domain.Seat;
import kr.co.victoryfairy.game.domain.SeatReader;
import kr.co.victoryfairy.game.domain.Team;
import kr.co.victoryfairy.game.domain.TeamReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonQueryService {

    private final TeamReader teamRepository;

    private final SeatReader seatRepository;

    public List<CommonDomain.TeamListResponse> findAll(MatchEnum.LeagueType league) {
        List<Team> teams = (league == null) ? teamRepository.findAllOrdered()
                : teamRepository.findByLeagueOrdered(league);

        return teams.stream()
            .map(entity -> new CommonDomain.TeamListResponse(entity.getId(), entity.getName(), entity.getLabel(),
                    entity.getLeague(), entity.getCountryCode()))
            .toList();
    }

    public List<CommonDomain.SeatListResponse> findSeat(Long id, String season) {
        List<Seat> seatEntities = seatRepository.findByStadiumAndSeason(id, season);
        if (seatEntities.isEmpty()) {
            return new ArrayList<>();
        }

        return seatEntities.stream()
            .map(entity -> new CommonDomain.SeatListResponse(entity.id(), entity.name()))
            .toList();
    }

}
