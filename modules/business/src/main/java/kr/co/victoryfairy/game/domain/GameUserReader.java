package kr.co.victoryfairy.game.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface GameUserReader {

    record Context(boolean memberExists, boolean profileExists, Long preferredTeamId) {
    }

    Context context(Long memberId);

    Optional<Long> preferredTeamId(Long memberId);

    Map<String, Long> diaryIdsByMatchId(Long memberId, Collection<String> matchIds);

}
