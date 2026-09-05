package kr.co.victoryfairy.game.domain;

import java.util.List;

public interface GameRecordRepository {

    record SavedRecords(List<HitterRecord> hitters, List<PitcherRecord> pitchers) {
    }

    List<HitterRecord> findHitters(String matchId);

    List<PitcherRecord> findPitchers(String matchId);

    SavedRecords save(String matchId, List<HitterRecord> hitters, List<PitcherRecord> pitchers);

}
