package kr.co.victoryfairy.diary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.GameRecordEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.GameRecordRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GameRecordDomainServiceTest {

    private final DiaryRepository diaryRepository = mock(DiaryRepository.class);

    private final GameRecordRepository gameRecordRepository = mock(GameRecordRepository.class);

    private final GameRecordDomainService service = new GameRecordDomainService(diaryRepository, gameRecordRepository);

    @Test
    void createsAResultForAnEndedMatch() {
        var away = new TeamEntity(1L, "KT", "KT");
        var home = new TeamEntity(2L, "LG", "LG");
        var match = match(MatchEnum.MatchStatus.END, away, home, (short) 6, (short) 13);
        var diary = diary(match, away);
        when(gameRecordRepository.findByDiaryEntityId(diary.getId())).thenReturn(Optional.empty());

        assertThat(service.record(diary)).isTrue();

        var record = ArgumentCaptor.forClass(GameRecordEntity.class);
        verify(gameRecordRepository).save(record.capture());
        assertThat(record.getValue().getResultType()).isEqualTo(MatchEnum.ResultType.LOSS);
        assertThat(record.getValue().getOpponentTeamEntity()).isSameAs(home);
        assertThat(diary.getIsRated()).isTrue();
    }

    @Test
    void recordsCanceledMatchesAsDrawWithoutScores() {
        var away = new TeamEntity(1L, "KT", "KT");
        var home = new TeamEntity(2L, "LG", "LG");
        var diary = diary(match(MatchEnum.MatchStatus.CANCELED, away, home, null, null), home);
        when(gameRecordRepository.findByDiaryEntityId(diary.getId())).thenReturn(Optional.empty());

        assertThat(service.record(diary)).isTrue();

        var record = ArgumentCaptor.forClass(GameRecordEntity.class);
        verify(gameRecordRepository).save(record.capture());
        assertThat(record.getValue().getResultType()).isEqualTo(MatchEnum.ResultType.DRAW);
    }

    @Test
    void leavesNonTerminalMatchesUnrated() {
        var away = new TeamEntity(1L, "KT", "KT");
        var home = new TeamEntity(2L, "LG", "LG");
        var diary = diary(match(MatchEnum.MatchStatus.PROGRESS, away, home, (short) 1, (short) 0), home);

        assertThat(service.record(diary)).isFalse();

        verify(gameRecordRepository, never()).save(org.mockito.ArgumentMatchers.any());
        assertThat(diary.getIsRated()).isFalse();
    }

    @Test
    void repairsTheRatedFlagWhenARecordAlreadyExists() {
        var away = new TeamEntity(1L, "KT", "KT");
        var home = new TeamEntity(2L, "LG", "LG");
        var diary = diary(match(MatchEnum.MatchStatus.END, away, home, (short) 1, (short) 0), home);
        when(gameRecordRepository.findByDiaryEntityId(diary.getId()))
            .thenReturn(Optional.of(GameRecordEntity.builder().diaryEntity(diary).build()));

        assertThat(service.record(diary)).isFalse();

        assertThat(diary.getIsRated()).isTrue();
        verify(diaryRepository).save(diary);
        verify(gameRecordRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void backfillsAllTerminalUnratedDiaries() {
        var away = new TeamEntity(1L, "KT", "KT");
        var home = new TeamEntity(2L, "LG", "LG");
        var diary = diary(match(MatchEnum.MatchStatus.END, away, home, (short) 2, (short) 1), home);
        when(diaryRepository.findByIsRatedFalseAndGameMatchEntityStatusIn(
                List.of(MatchEnum.MatchStatus.END, MatchEnum.MatchStatus.CANCELED)))
            .thenReturn(List.of(diary));
        when(gameRecordRepository.findByDiaryEntityId(diary.getId())).thenReturn(Optional.empty());

        assertThat(service.recoverAllTerminal()).isEqualTo(1);
    }

    private static DiaryEntity diary(GameMatchEntity match, TeamEntity team) {
        return DiaryEntity.builder()
            .id(11L)
            .member(MemberEntity.builder().id(7L).build())
            .gameMatchEntity(match)
            .teamEntity(team)
            .isRated(false)
            .build();
    }

    private static GameMatchEntity match(MatchEnum.MatchStatus status, TeamEntity away, TeamEntity home,
            Short awayScore, Short homeScore) {
        return GameMatchEntity.builder()
            .id("20260827HHSK0")
            .season("2026")
            .league(MatchEnum.LeagueType.KBO)
            .awayTeamEntity(away)
            .awayScore(awayScore)
            .homeTeamEntity(home)
            .homeScore(homeScore)
            .status(status)
            .build();
    }

}
