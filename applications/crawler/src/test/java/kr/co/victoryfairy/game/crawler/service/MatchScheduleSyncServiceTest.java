package kr.co.victoryfairy.game.crawler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class MatchScheduleSyncServiceTest {

    @Test
    void updatesExistingMatchInPlaceInsteadOfReplacingItsIdentity() {
        GameMatchRepository repository = Mockito.mock(GameMatchRepository.class);
        GameMatchEntity existing = GameMatchEntity.builder()
            .id("20260819KTLG0")
            .status(MatchEnum.MatchStatus.READY)
            .isMatchInfoCraw(true)
            .build();
        GameMatchEntity official = GameMatchEntity.builder()
            .id("20260819KTLG0")
            .matchAt(LocalDateTime.of(2026, 8, 19, 19, 0))
            .awayScore((short) 0)
            .homeScore((short) 1)
            .status(MatchEnum.MatchStatus.END)
            .build();
        when(repository.findById("20260819KTLG0")).thenReturn(Optional.of(existing));

        new MatchScheduleSyncService(repository).sync(List.of(official));

        ArgumentCaptor<List<GameMatchEntity>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).containsExactly(existing);
        assertThat(existing.getStatus()).isEqualTo(MatchEnum.MatchStatus.END);
        assertThat(existing.getIsMatchInfoCraw()).isTrue();
    }

}
