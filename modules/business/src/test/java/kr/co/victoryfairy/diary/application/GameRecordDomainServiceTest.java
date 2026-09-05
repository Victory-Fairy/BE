package kr.co.victoryfairy.diary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.co.victoryfairy.diary.domain.Diary;
import kr.co.victoryfairy.diary.domain.DiaryEnum;
import kr.co.victoryfairy.diary.domain.DiaryStore;
import kr.co.victoryfairy.diary.domain.GameRecord;
import kr.co.victoryfairy.diary.domain.GameRecordStore;
import kr.co.victoryfairy.game.domain.GameMatch;
import kr.co.victoryfairy.game.domain.GameMatchRepository;
import kr.co.victoryfairy.game.domain.MatchEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GameRecordDomainServiceTest {
    private final DiaryStore diaries = mock(DiaryStore.class);
    private final GameRecordStore records = mock(GameRecordStore.class);
    private final GameMatchRepository matches = mock(GameMatchRepository.class);
    private final GameRecordDomainService service = new GameRecordDomainService(diaries, records, matches);

    @Test
    void createsAResultForAnEndedMatch() {
        var diary = diary(1L);
        when(matches.findById("match")).thenReturn(Optional.of(match(MatchEnum.MatchStatus.END, (short) 6, (short) 13)));
        when(records.findByDiaryId(11L)).thenReturn(Optional.empty());
        assertThat(service.record(diary)).isTrue();
        var saved = ArgumentCaptor.forClass(GameRecord.class);
        verify(records).save(saved.capture());
        assertThat(saved.getValue().result()).isEqualTo(MatchEnum.ResultType.LOSS);
        assertThat(saved.getValue().opponentTeamId()).isEqualTo(2L);
        verify(diaries).save(diary.markRated());
    }

    @Test
    void recordsCanceledMatchesAsDrawWithoutScores() {
        var diary = diary(2L);
        when(matches.findById("match")).thenReturn(Optional.of(match(MatchEnum.MatchStatus.CANCELED, null, null)));
        when(records.findByDiaryId(11L)).thenReturn(Optional.empty());
        assertThat(service.record(diary)).isTrue();
        var saved = ArgumentCaptor.forClass(GameRecord.class);
        verify(records).save(saved.capture());
        assertThat(saved.getValue().result()).isEqualTo(MatchEnum.ResultType.DRAW);
    }

    @Test
    void leavesNonTerminalMatchesUnrated() {
        var diary = diary(2L);
        when(matches.findById("match")).thenReturn(Optional.of(match(MatchEnum.MatchStatus.PROGRESS, (short) 1, (short) 0)));
        assertThat(service.record(diary)).isFalse();
        verify(records, never()).save(any());
        verify(diaries, never()).save(any());
    }

    @Test
    void repairsTheRatedFlagWhenARecordAlreadyExists() {
        var diary = diary(2L);
        when(matches.findById("match")).thenReturn(Optional.of(match(MatchEnum.MatchStatus.END, (short) 1, (short) 0)));
        when(records.findByDiaryId(11L)).thenReturn(Optional.of(mock(GameRecord.class)));
        assertThat(service.record(diary)).isFalse();
        verify(diaries).save(diary.markRated());
        verify(records, never()).save(any());
    }

    @Test
    void backfillsAllTerminalUnratedDiaries() {
        var diary = diary(2L);
        when(diaries.findAllUnratedTerminal()).thenReturn(List.of(diary));
        when(matches.findById("match")).thenReturn(Optional.of(match(MatchEnum.MatchStatus.END, (short) 2, (short) 1)));
        when(records.findByDiaryId(11L)).thenReturn(Optional.empty());
        assertThat(service.recoverAllTerminal()).isEqualTo(1);
    }

    private static Diary diary(Long teamId) {
        return new Diary(11L, 7L, "match", teamId, teamId == 1 ? "away" : "home", DiaryEnum.ViewType.HOME,
                null, null, null, false, null, null);
    }

    private static GameMatch match(MatchEnum.MatchStatus status, Short away, Short home) {
        return new GameMatch("match", MatchEnum.LeagueType.KBO, null, null, "2026", LocalDateTime.MIN, 1L,
                "away", away, 2L, "home", home, 3L, status, null, false, false, true, null, null);
    }
}
