package kr.co.victoryfairy.diary.domain;

import java.time.LocalDateTime;
import kr.co.victoryfairy.game.domain.MatchEnum;

public record GameRecord(Long id, Long memberId, Long diaryId, String gameMatchId, Long teamId, String teamName,
        Long opponentTeamId, String opponentTeamName, Long stadiumId, String stadiumName,
        DiaryEnum.ViewType viewType, MatchEnum.MatchStatus status, MatchEnum.ResultType result, String season,
        MatchEnum.LeagueType league, LocalDateTime matchAt, Long homeTeamId, LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public GameRecord switchTeam(Long nextTeamId, String nextTeamName) {
        var nextResult = result == MatchEnum.ResultType.WIN ? MatchEnum.ResultType.LOSS
                : result == MatchEnum.ResultType.LOSS ? MatchEnum.ResultType.WIN : result;
        return new GameRecord(id, memberId, diaryId, gameMatchId, nextTeamId, nextTeamName, teamId, teamName, stadiumId,
                stadiumName, viewType, status, nextResult, season, league, matchAt, homeTeamId, createdAt, updatedAt);
    }
}
