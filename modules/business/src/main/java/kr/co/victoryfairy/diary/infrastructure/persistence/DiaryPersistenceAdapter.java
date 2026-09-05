package kr.co.victoryfairy.diary.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import kr.co.victoryfairy.diary.domain.Diary;
import kr.co.victoryfairy.diary.domain.DiaryStore;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.TeamRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberRepository;
import org.springframework.stereotype.Repository;

@Repository
public class DiaryPersistenceAdapter implements DiaryStore {
    private static final List<MatchEnum.MatchStatus> TERMINAL =
            List.of(MatchEnum.MatchStatus.END, MatchEnum.MatchStatus.CANCELED);
    private final kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository diaries;
    private final MemberRepository members;
    private final GameMatchRepository matches;
    private final TeamRepository teams;

    public DiaryPersistenceAdapter(kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository diaries,
            MemberRepository members, GameMatchRepository matches, TeamRepository teams) {
        this.diaries = diaries;
        this.members = members;
        this.matches = matches;
        this.teams = teams;
    }

    public Optional<Diary> findByMemberAndMatch(Long memberId, String matchId) {
        return diaries.findByMemberIdAndGameMatchEntityId(memberId, matchId).map(DiaryPersistenceMapper::toDomain);
    }

    public Optional<Diary> findByMemberAndId(Long memberId, Long diaryId) {
        return diaries.findByMemberIdAndId(memberId, diaryId).map(DiaryPersistenceMapper::toDomain);
    }

    public Optional<Diary> findDetailByMemberAndId(Long memberId, Long diaryId) {
        return diaries.findDetailByMemberIdAndId(memberId, diaryId).map(DiaryPersistenceMapper::toDomain);
    }

    public List<Diary> findUnratedByMatch(String matchId) {
        return diaries.findByGameMatchEntityIdAndIsRatedFalse(matchId).stream().map(DiaryPersistenceMapper::toDomain).toList();
    }

    public List<Diary> findAllUnratedTerminal() {
        return diaries.findByIsRatedFalseAndGameMatchEntityStatusIn(TERMINAL).stream().map(DiaryPersistenceMapper::toDomain).toList();
    }

    public Diary save(Diary value) {
        DiaryEntity entity = value.id() == null ? DiaryEntity.builder().build()
                : diaries.findById(value.id()).orElseThrow();
        if (value.id() != null && !java.util.Objects.equals(value.updatedAt(), entity.getUpdatedAt())) entity.update();
        entity.apply(value, members.getReferenceById(value.memberId()), matches.getReferenceById(value.gameMatchId()),
                teams.getReferenceById(value.teamId()));
        return DiaryPersistenceMapper.toDomain(diaries.save(entity));
    }

    public void delete(Long diaryId) {
        diaries.deleteById(diaryId);
    }
}
