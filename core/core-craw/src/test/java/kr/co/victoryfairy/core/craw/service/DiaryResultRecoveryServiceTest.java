package kr.co.victoryfairy.core.craw.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.storage.db.core.entity.DiaryEntity;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.GameRecordEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import kr.co.victoryfairy.storage.db.core.entity.TeamEntity;
import kr.co.victoryfairy.storage.db.core.repository.DiaryRepository;
import kr.co.victoryfairy.storage.db.core.repository.GameRecordRepository;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class DiaryResultRecoveryServiceTest {

    @Test
    void createsMissingGameRecordFromRecoveredFinalScore() {
        DiaryRepository diaryRepository = Mockito.mock(DiaryRepository.class);
        GameRecordRepository recordRepository = Mockito.mock(GameRecordRepository.class);
        GameMatchRepository matchRepository = Mockito.mock(GameMatchRepository.class);
        MemberEntity member = MemberEntity.builder().id(7L).build();
        TeamEntity away = new TeamEntity(1L, "KT", "KT");
        TeamEntity home = new TeamEntity(2L, "LG", "LG");
        GameMatchEntity match = GameMatchEntity.builder()
            .id("20260819KTLG0")
            .season("2026")
            .league(MatchEnum.LeagueType.KBO)
            .awayTeamEntity(away)
            .awayScore((short) 0)
            .homeTeamEntity(home)
            .homeScore((short) 1)
            .status(MatchEnum.MatchStatus.END)
            .build();
        DiaryEntity diary = DiaryEntity.builder()
            .id(11L)
            .member(member)
            .gameMatchEntity(match)
            .teamEntity(home)
            .isRated(false)
            .build();
        when(diaryRepository.findByGameMatchEntityAndIsRatedFalse(match)).thenReturn(List.of(diary));
        when(matchRepository.findById(match.getId())).thenReturn(java.util.Optional.of(match));

        int recovered = new DiaryResultRecoveryService(diaryRepository, recordRepository, matchRepository)
            .recover(match.getId());

        ArgumentCaptor<GameRecordEntity> record = ArgumentCaptor.forClass(GameRecordEntity.class);
        verify(recordRepository).save(record.capture());
        assertThat(record.getValue().getResultType()).isEqualTo(MatchEnum.ResultType.WIN);
        assertThat(record.getValue().getDiaryEntity()).isSameAs(diary);
        assertThat(diary.getIsRated()).isTrue();
        assertThat(recovered).isEqualTo(1);
    }

    @Test
    void doesNotCreateRecordsForCanceledMatches() {
        DiaryRepository diaryRepository = Mockito.mock(DiaryRepository.class);
        GameRecordRepository recordRepository = Mockito.mock(GameRecordRepository.class);
        GameMatchRepository matchRepository = Mockito.mock(GameMatchRepository.class);
        GameMatchEntity canceled = GameMatchEntity.builder().id("canceled").status(MatchEnum.MatchStatus.CANCELED).build();
        when(matchRepository.findById(canceled.getId())).thenReturn(java.util.Optional.of(canceled));

        int recovered = new DiaryResultRecoveryService(diaryRepository, recordRepository, matchRepository)
            .recover(canceled.getId());

        verify(recordRepository, never()).save(Mockito.any());
        assertThat(recovered).isZero();
    }

}
