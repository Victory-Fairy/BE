package kr.co.victoryfairy.game.infrastructure.persistence;

import java.util.List;
import kr.co.victoryfairy.game.domain.GameRecordRepository;
import kr.co.victoryfairy.game.domain.HitterRecord;
import kr.co.victoryfairy.game.domain.PitcherRecord;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.HitterRecordEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.PitcherRecordEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.HitterRecordRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.PitcherRecordRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchRepository;
import org.springframework.stereotype.Repository;

@Repository
public class GameRecordPersistenceAdapter implements GameRecordRepository {

    private final HitterRecordRepository hitters;

    private final PitcherRecordRepository pitchers;

    private final GameMatchRepository matches;

    public GameRecordPersistenceAdapter(HitterRecordRepository hitters, PitcherRecordRepository pitchers,
            GameMatchRepository matches) {
        this.hitters = hitters;
        this.pitchers = pitchers;
        this.matches = matches;
    }

    public List<HitterRecord> findHitters(String matchId) {
        return hitters.findByGameMatchEntityId(matchId).stream().map(GamePersistenceMapper::toDomain).toList();
    }

    public List<PitcherRecord> findPitchers(String matchId) {
        return pitchers.findByGameMatchEntityId(matchId).stream().map(GamePersistenceMapper::toDomain).toList();
    }

    public SavedRecords save(String matchId, List<HitterRecord> hitterValues, List<PitcherRecord> pitcherValues) {
        var match = matches.getReferenceById(matchId);
        var savedHitters = hitters
            .saveAll(hitterValues.stream()
                .map(v -> new HitterRecordEntity(v.turn(), v.name(), v.position(), v.hitCount(), v.score(), v.hit(),
                        v.homeRun(), v.hitScore(), v.ballFour(), v.strikeOut(), match, v.season(), v.home()))
                .toList())
            .stream()
            .map(GamePersistenceMapper::toDomain)
            .toList();
        var savedPitchers = pitchers
            .saveAll(pitcherValues.stream()
                .map(v -> new PitcherRecordEntity(v.turn(), v.name(), v.position(), v.inning(), v.pitching(),
                        v.ballFour(), v.strikeOut(), v.hit(), v.homeRun(), v.score(), match, v.season(), v.home()))
                .toList())
            .stream()
            .map(GamePersistenceMapper::toDomain)
            .toList();
        return new SavedRecords(savedHitters, savedPitchers);
    }

}
