package kr.co.victoryfairy.game.infrastructure.persistence;

import java.util.List;
import kr.co.victoryfairy.game.domain.Seat;
import kr.co.victoryfairy.game.domain.SeatReader;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.SeatRepository;
import org.springframework.stereotype.Repository;

@Repository
public class SeatPersistenceAdapter implements SeatReader {

    private final SeatRepository seats;

    public SeatPersistenceAdapter(SeatRepository seats) {
        this.seats = seats;
    }

    public List<Seat> findByStadiumAndSeason(Long stadiumId, String season) {
        return seats.findByStadiumEntityIdAndSeason(stadiumId, season)
            .stream()
            .map(GamePersistenceMapper::toDomain)
            .toList();
    }

}
