package kr.co.victoryfairy.diary.infrastructure.persistence;

import kr.co.victoryfairy.diary.domain.Diary;
import kr.co.victoryfairy.diary.domain.GameRecord;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.GameRecordEntity;

final class DiaryPersistenceMapper {
    private DiaryPersistenceMapper() {}

    static Diary toDomain(DiaryEntity entity) {
        return new Diary(entity.getId(), entity.getMemberId(), entity.getGameMatchEntity().getId(),
                entity.getTeamEntity().getId(), entity.getTeamName(), entity.getViewType(), entity.getWeatherType(),
                entity.getMoodType(), entity.getContent(), entity.getIsRated(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    static GameRecord toDomain(GameRecordEntity entity) {
        var match = entity.getGameMatchEntity();
        return new GameRecord(entity.getId(), entity.getMemberId(), entity.getDiaryEntity().getId(),
                match.getId(), entity.getTeamEntity().getId(), entity.getTeamName(),
                entity.getOpponentTeamEntity().getId(), entity.getOpponentTeamName(), entity.getStadiumEntity().getId(),
                entity.getStadiumEntity().getFullName(), entity.getViewType(), entity.getStatus(),
                entity.getResultType(), entity.getSeason(), entity.getLeagueType(), match.getMatchAt(),
                match.getHomeTeamEntity().getId(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
