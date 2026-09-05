package kr.co.victoryfairy.game.infrastructure.persistence;

import kr.co.victoryfairy.game.domain.GameMatch;
import kr.co.victoryfairy.game.domain.HitterRecord;
import kr.co.victoryfairy.game.domain.PitcherRecord;
import kr.co.victoryfairy.game.domain.Stadium;
import kr.co.victoryfairy.game.domain.Seat;
import kr.co.victoryfairy.game.domain.Team;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.HitterRecordEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.PitcherRecordEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.StadiumEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.SeatEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;

public final class GamePersistenceMapper {

    private GamePersistenceMapper() {
    }

    public static GameMatch toDomain(GameMatchEntity e) {
        return new GameMatch(e.getId(), e.getLeague(), e.getType(), e.getSeries(), e.getSeason(), e.getMatchAt(),
                e.getAwayTeamEntity() == null ? null : e.getAwayTeamEntity().getId(), e.getAwayNm(), e.getAwayScore(),
                e.getHomeTeamEntity() == null ? null : e.getHomeTeamEntity().getId(), e.getHomeNm(), e.getHomeScore(),
                e.getStadiumEntity() == null ? null : e.getStadiumEntity().getId(), e.getStatus(), e.getReason(),
                e.getIsMatchInfoCraw(), e.getIsSendPush(), e.getIsUse(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public static Team toDomain(TeamEntity e) {
        return new Team(e.getId(), e.getName(), e.getKboNm(), e.getSponsorNm(), e.getLabel(), e.getOrderNo(),
                e.getLeague(), e.getCountryCode(), e.getIsUse(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public static Stadium toDomain(StadiumEntity e) {
        return new Stadium(e.getId(), e.getFullName(), e.getShortName(), e.getRegion(), e.getExternalId(), e.getIsUse(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    public static Seat toDomain(SeatEntity e) {
        return new Seat(e.getId(), e.getStadiumEntity() == null ? null : e.getStadiumEntity().getId(), e.getName(),
                e.getSeason(), e.getIsUse(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public static HitterRecord toDomain(HitterRecordEntity e) {
        return new HitterRecord(e.getId(), e.getTurn(), e.getName(), e.getPosition(), e.getHitCount(), e.getScore(),
                e.getHit(), e.getHomeRun(), e.getHitScore(), e.getBallFour(), e.getStrikeOut(),
                e.getGameMatchEntity().getId(), e.getSeason(), e.getHome());
    }

    public static PitcherRecord toDomain(PitcherRecordEntity e) {
        return new PitcherRecord(e.getId(), e.getTurn(), e.getName(), e.getPosition(), e.getInning(), e.getPitching(),
                e.getBallFour(), e.getStrikeOut(), e.getHit(), e.getHomeRun(), e.getScore(),
                e.getGameMatchEntity().getId(), e.getSeason(), e.getHome());
    }

}
