package kr.co.victoryfairy.game.domain;

import java.util.List;

public interface SeatReader {

    List<Seat> findByStadiumAndSeason(Long stadiumId, String season);

}
