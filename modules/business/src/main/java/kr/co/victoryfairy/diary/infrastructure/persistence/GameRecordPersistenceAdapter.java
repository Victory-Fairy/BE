package kr.co.victoryfairy.diary.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import kr.co.victoryfairy.diary.domain.GameRecord;
import kr.co.victoryfairy.diary.domain.GameRecordStore;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.GameRecordEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.GameRecordRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.StadiumRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.TeamRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberRepository;
import org.springframework.stereotype.Repository;

@Repository("diaryGameRecordPersistenceAdapter")
public class GameRecordPersistenceAdapter implements GameRecordStore {
    private final GameRecordRepository records;
    private final MemberRepository members;
    private final DiaryRepository diaries;
    private final GameMatchRepository matches;
    private final TeamRepository teams;
    private final StadiumRepository stadiums;

    public GameRecordPersistenceAdapter(GameRecordRepository records, MemberRepository members, DiaryRepository diaries,
            GameMatchRepository matches, TeamRepository teams, StadiumRepository stadiums) {
        this.records = records;
        this.members = members;
        this.diaries = diaries;
        this.matches = matches;
        this.teams = teams;
        this.stadiums = stadiums;
    }

    public Optional<GameRecord> findByDiaryId(Long diaryId) {
        return records.findByDiaryEntityId(diaryId).map(DiaryPersistenceMapper::toDomain);
    }

    public List<GameRecord> findByMemberAndSeason(Long memberId, String season) {
        return records.findByMemberAndSeason(members.getReferenceById(memberId), season).stream().map(DiaryPersistenceMapper::toDomain).toList();
    }

    public List<GameRecord> findByMemberAndSeasonOrdered(Long memberId, String season) {
        return records.findByMemberIdAndSeasonOrderByGameMatchEntityMatchAtAsc(memberId, season).stream().map(DiaryPersistenceMapper::toDomain).toList();
    }

    public GameRecord save(GameRecord value) {
        GameRecordEntity entity = value.id() == null ? GameRecordEntity.builder().build()
                : records.findById(value.id()).orElseThrow();
        entity.apply(value, members.getReferenceById(value.memberId()), diaries.getReferenceById(value.diaryId()),
                matches.getReferenceById(value.gameMatchId()), teams.getReferenceById(value.teamId()),
                teams.getReferenceById(value.opponentTeamId()), stadiums.getReferenceById(value.stadiumId()));
        return DiaryPersistenceMapper.toDomain(records.save(entity));
    }

    public void delete(Long id) { records.deleteById(id); }
}
