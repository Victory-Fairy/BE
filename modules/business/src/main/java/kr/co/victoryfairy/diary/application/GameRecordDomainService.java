package kr.co.victoryfairy.diary.application;

import kr.co.victoryfairy.diary.domain.Diary;
import kr.co.victoryfairy.diary.domain.DiaryStore;
import kr.co.victoryfairy.diary.domain.GameRecord;
import kr.co.victoryfairy.diary.domain.GameRecordStore;
import kr.co.victoryfairy.game.domain.GameMatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameRecordDomainService {
    private final DiaryStore diaries;
    private final GameRecordStore records;
    private final GameMatchRepository matches;

    @Transactional
    public boolean record(Diary diary) {
        var match = matches.findById(diary.gameMatchId()).orElseThrow();
        if (!Diary.isRecordable(match.status(), match.awayScore(), match.homeScore())) return false;
        if (records.findByDiaryId(diary.id()).isPresent()) {
            if (!Boolean.TRUE.equals(diary.rated())) diaries.save(diary.markRated());
            return false;
        }
        boolean away = match.awayTeamId().equals(diary.teamId());
        Long opponentId = away ? match.homeTeamId() : match.awayTeamId();
        String opponentName = away ? match.homeName() : match.awayName();
        records.save(new GameRecord(null, diary.memberId(), diary.id(), match.id(), diary.teamId(), diary.teamName(),
                opponentId, opponentName, match.stadiumId(), null, diary.viewType(), match.status(),
                Diary.result(match.status(), match.result(diary.teamId().equals(match.homeTeamId()))), match.season(),
                match.league(), match.matchAt(),
                match.homeTeamId(), null, null));
        diaries.save(diary.markRated());
        return true;
    }

    @Transactional
    public int recover(String matchId) {
        return diaries.findUnratedByMatch(matchId).stream().mapToInt(diary -> record(diary) ? 1 : 0).sum();
    }

    @Transactional
    public int recoverAllTerminal() {
        return diaries.findAllUnratedTerminal().stream().mapToInt(diary -> record(diary) ? 1 : 0).sum();
    }
}
