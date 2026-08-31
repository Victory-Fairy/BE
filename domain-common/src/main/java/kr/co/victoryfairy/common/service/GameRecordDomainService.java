package kr.co.victoryfairy.common.service;

import java.util.List;

import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.storage.db.core.entity.DiaryEntity;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.GameRecordEntity;
import kr.co.victoryfairy.storage.db.core.repository.DiaryRepository;
import kr.co.victoryfairy.storage.db.core.repository.GameRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameRecordDomainService {

    private static final List<MatchEnum.MatchStatus> TERMINAL_STATUSES = List.of(MatchEnum.MatchStatus.END,
            MatchEnum.MatchStatus.CANCELED);

    private final DiaryRepository diaryRepository;

    private final GameRecordRepository gameRecordRepository;

    @Transactional
    public boolean record(DiaryEntity diary) {
        GameMatchEntity match = diary.getGameMatchEntity();
        if (!TERMINAL_STATUSES.contains(match.getStatus()) || !hasFinalResult(match)) {
            return false;
        }

        if (gameRecordRepository.findByDiaryEntityId(diary.getId()).isPresent()) {
            if (!Boolean.TRUE.equals(diary.getIsRated())) {
                diary.updateRated();
                diaryRepository.save(diary);
            }
            return false;
        }

        var team = diary.getTeamEntity();
        boolean isAway = match.getAwayTeamEntity().getId().equals(team.getId());
        var opponent = isAway ? match.getHomeTeamEntity() : match.getAwayTeamEntity();

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
            .resultType(result(match, isAway))
            .season(match.getSeason())
            .leagueType(match.getLeague())
            .build());
        diary.updateRated();
        diaryRepository.save(diary);
        return true;
    }

    @Transactional
    public int recover(GameMatchEntity match) {
        return diaryRepository.findByGameMatchEntityAndIsRatedFalse(match)
            .stream()
            .mapToInt(diary -> record(diary) ? 1 : 0)
            .sum();
    }

    @Transactional
    public int recoverAllTerminal() {
        return diaryRepository.findByIsRatedFalseAndGameMatchEntityStatusIn(TERMINAL_STATUSES)
            .stream()
            .mapToInt(diary -> record(diary) ? 1 : 0)
            .sum();
    }

    private static boolean hasFinalResult(GameMatchEntity match) {
        return match.getStatus() == MatchEnum.MatchStatus.CANCELED
                || match.getAwayScore() != null && match.getHomeScore() != null;
    }

    private static MatchEnum.ResultType result(GameMatchEntity match, boolean isAway) {
        if (match.getStatus() == MatchEnum.MatchStatus.CANCELED) {
            return MatchEnum.ResultType.DRAW;
        }

        short myScore = isAway ? match.getAwayScore() : match.getHomeScore();
        short opponentScore = isAway ? match.getHomeScore() : match.getAwayScore();
        return myScore == opponentScore ? MatchEnum.ResultType.DRAW
                : myScore > opponentScore ? MatchEnum.ResultType.WIN : MatchEnum.ResultType.LOSS;
    }

}
