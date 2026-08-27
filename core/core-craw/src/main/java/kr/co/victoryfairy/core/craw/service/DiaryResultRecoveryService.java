package kr.co.victoryfairy.core.craw.service;

import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.GameRecordEntity;
import kr.co.victoryfairy.storage.db.core.repository.DiaryRepository;
import kr.co.victoryfairy.storage.db.core.repository.GameRecordRepository;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiaryResultRecoveryService {

    private final DiaryRepository diaryRepository;

    private final GameRecordRepository gameRecordRepository;

    private final GameMatchRepository gameMatchRepository;

    public DiaryResultRecoveryService(DiaryRepository diaryRepository, GameRecordRepository gameRecordRepository,
            GameMatchRepository gameMatchRepository) {
        this.diaryRepository = diaryRepository;
        this.gameRecordRepository = gameRecordRepository;
        this.gameMatchRepository = gameMatchRepository;
    }

    @Transactional
    public int recover(String matchId) {
        GameMatchEntity match = gameMatchRepository.findById(matchId).orElseThrow();
        if (match.getStatus() != MatchEnum.MatchStatus.END || match.getAwayScore() == null
                || match.getHomeScore() == null) {
            return 0;
        }

        int recovered = 0;
        for (var diary : diaryRepository.findByGameMatchEntityAndIsRatedFalse(match)) {
            if (gameRecordRepository.findByMemberAndDiaryEntityId(diary.getMember(), diary.getId()) != null) {
                diary.updateRated();
                diaryRepository.save(diary);
                continue;
            }

            var team = diary.getTeamEntity();
            boolean isAway = match.getAwayTeamEntity().getId().equals(team.getId());
            var opponent = isAway ? match.getHomeTeamEntity() : match.getAwayTeamEntity();
            short myScore = isAway ? match.getAwayScore() : match.getHomeScore();
            short opponentScore = isAway ? match.getHomeScore() : match.getAwayScore();
            MatchEnum.ResultType result = myScore == opponentScore ? MatchEnum.ResultType.DRAW
                    : myScore > opponentScore ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS;

            gameRecordRepository.save(GameRecordEntity.builder()
                .member(diary.getMember())
                .diaryEntity(diary)
                .gameMatchEntity(match)
                .teamEntity(team)
                .teamName(team.getName())
                .opponentTeamEntity(opponent)
                .opponentTeamName(opponent.getName())
                .stadiumEntity(match.getStadiumEntity())
                .viewType(diary.getViewType())
                .status(match.getStatus())
                .resultType(result)
                .season(match.getSeason())
                .leagueType(match.getLeague())
                .build());
            diary.updateRated();
            diaryRepository.save(diary);
            recovered++;
        }
        return recovered;
    }

}
