package kr.co.victoryfairy.diary.domain;

import java.time.LocalDateTime;
import kr.co.victoryfairy.game.domain.MatchEnum;

public record Diary(Long id, Long memberId, String gameMatchId, Long teamId, String teamName,
        DiaryEnum.ViewType viewType, DiaryEnum.WeatherType weather, DiaryEnum.MoodType mood, String content,
        Boolean rated, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public Diary update(Long teamId, String teamName, DiaryEnum.ViewType viewType, DiaryEnum.MoodType mood,
            DiaryEnum.WeatherType weather, String content) {
        return new Diary(id, memberId, gameMatchId, teamId, teamName, viewType, weather, mood, content, rated,
                createdAt, LocalDateTime.now());
    }

    public Diary markRated() {
        return Boolean.TRUE.equals(rated) ? this
                : new Diary(id, memberId, gameMatchId, teamId, teamName, viewType, weather, mood, content, true,
                        createdAt, updatedAt);
    }

    public static boolean isRecordable(MatchEnum.MatchStatus status, Short awayScore, Short homeScore) {
        return status == MatchEnum.MatchStatus.CANCELED
                || status == MatchEnum.MatchStatus.END && awayScore != null && homeScore != null;
    }

    public static MatchEnum.ResultType result(MatchEnum.MatchStatus status, MatchEnum.ResultType playedResult) {
        if (status == MatchEnum.MatchStatus.CANCELED)
            return MatchEnum.ResultType.DRAW;
        return playedResult;
    }
}
