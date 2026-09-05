package kr.co.victoryfairy.game.domain;

import java.time.LocalDateTime;

public record GameMatch(String id, MatchEnum.LeagueType league, MatchEnum.MatchType type, MatchEnum.SeriesType series,
        String season, LocalDateTime matchAt, Long awayTeamId, String awayName, Short awayScore, Long homeTeamId,
        String homeName, Short homeScore, Long stadiumId, MatchEnum.MatchStatus status, String reason,
        Boolean detailCrawled, Boolean pushSent, Boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public String getId() {
        return id;
    }

    public MatchEnum.LeagueType getLeague() {
        return league;
    }

    public MatchEnum.MatchType getType() {
        return type;
    }

    public MatchEnum.SeriesType getSeries() {
        return series;
    }

    public String getSeason() {
        return season;
    }

    public LocalDateTime getMatchAt() {
        return matchAt;
    }

    public Long getAwayTeamId() {
        return awayTeamId;
    }

    public String getAwayNm() {
        return awayName;
    }

    public Short getAwayScore() {
        return awayScore;
    }

    public Long getHomeTeamId() {
        return homeTeamId;
    }

    public String getHomeNm() {
        return homeName;
    }

    public Short getHomeScore() {
        return homeScore;
    }

    public Long getStadiumId() {
        return stadiumId;
    }

    public MatchEnum.MatchStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Boolean getIsMatchInfoCraw() {
        return detailCrawled;
    }

    public Boolean getIsSendPush() {
        return pushSent;
    }

    public GameMatch syncSchedule(GameMatch source) {
        return new GameMatch(id, source.league, source.type, source.series, source.season, source.matchAt,
                source.awayTeamId, source.awayName, source.awayScore, source.homeTeamId, source.homeName,
                source.homeScore, source.stadiumId, source.status, source.reason, detailCrawled, pushSent, active,
                createdAt, updatedAt);
    }

    public GameMatch updateLive(MatchEnum.MatchStatus status, String reason, Short awayScore, Short homeScore) {
        return new GameMatch(id, league, type, series, season, matchAt, awayTeamId, awayName, awayScore, homeTeamId,
                homeName, homeScore, stadiumId, status, reason, detailCrawled, pushSent, active, createdAt, updatedAt);
    }

    public GameMatch markDetailCrawled() {
        return new GameMatch(id, league, type, series, season, matchAt, awayTeamId, awayName, awayScore, homeTeamId,
                homeName, homeScore, stadiumId, status, reason, true, pushSent, active, createdAt, updatedAt);
    }

    public MatchEnum.ResultType result(boolean home) {
        Short own = home ? homeScore : awayScore;
        Short other = home ? awayScore : homeScore;
        if (own == null || other == null)
            return null;
        int comparison = Short.compare(own, other);
        return comparison == 0 ? MatchEnum.ResultType.DRAW
                : comparison > 0 ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS;
    }
}
