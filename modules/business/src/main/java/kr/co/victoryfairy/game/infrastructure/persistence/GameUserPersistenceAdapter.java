package kr.co.victoryfairy.game.infrastructure.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository;
import kr.co.victoryfairy.game.domain.GameUserReader;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberInfoEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberInfoRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberRepository;
import org.springframework.stereotype.Repository;

@Repository
public class GameUserPersistenceAdapter implements GameUserReader {

    private final MemberInfoRepository members;

    private final MemberRepository memberRepository;

    private final DiaryRepository diaries;

    public GameUserPersistenceAdapter(MemberInfoRepository members, MemberRepository memberRepository,
            DiaryRepository diaries) {
        this.members = members;
        this.memberRepository = memberRepository;
        this.diaries = diaries;
    }

    public Context context(Long memberId) {
        var member = memberRepository.findById(memberId);
        if (member.isEmpty())
            return new Context(false, false, null);
        var profile = members.findByMemberEntity(member.get());
        return new Context(true, profile.isPresent(),
                profile.map(MemberInfoEntity::getTeamEntity).map(team -> team.getId()).orElse(null));
    }

    public Optional<Long> preferredTeamId(Long memberId) {
        return members.findByMemberEntity_Id(memberId).map(MemberInfoEntity::getTeamEntity).map(team -> team.getId());
    }

    public Map<String, Long> diaryIdsByMatchId(Long memberId, Collection<String> matchIds) {
        if (memberId == null || matchIds.isEmpty())
            return Map.of();
        return diaries.findByMemberIdAndGameMatchEntityIdIn(memberId, matchIds)
            .stream()
            .collect(Collectors.toMap(diary -> diary.getGameMatchEntity().getId(), diary -> diary.getId()));
    }

}
