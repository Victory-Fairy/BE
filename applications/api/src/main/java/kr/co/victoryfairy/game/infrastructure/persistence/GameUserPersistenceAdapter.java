package kr.co.victoryfairy.game.infrastructure.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository;
import kr.co.victoryfairy.game.domain.GameUserReader;
import kr.co.victoryfairy.member.domain.MemberStore;
import org.springframework.stereotype.Repository;

@Repository
public class GameUserPersistenceAdapter implements GameUserReader {

    private final MemberStore members;

    private final DiaryRepository diaries;

    public GameUserPersistenceAdapter(MemberStore members, DiaryRepository diaries) {
        this.members = members;
        this.diaries = diaries;
    }

    public Context context(Long memberId) {
        if (!members.memberExists(memberId))
            return new Context(false, false, null);
        var profile = members.findProfileByMemberId(memberId);
        return new Context(true, profile.isPresent(),
                profile.map(profileValue -> profileValue.teamId()).orElse(null));
    }

    public Optional<Long> preferredTeamId(Long memberId) {
        return members.findProfileByMemberId(memberId).map(profile -> profile.teamId());
    }

    public Map<String, Long> diaryIdsByMatchId(Long memberId, Collection<String> matchIds) {
        if (memberId == null || matchIds.isEmpty())
            return Map.of();
        return diaries.findByMemberIdAndGameMatchEntityIdIn(memberId, matchIds)
            .stream()
            .collect(Collectors.toMap(diary -> diary.getGameMatchEntity().getId(), diary -> diary.getId()));
    }

}
