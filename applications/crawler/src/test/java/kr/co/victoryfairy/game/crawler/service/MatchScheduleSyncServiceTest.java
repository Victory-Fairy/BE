package kr.co.victoryfairy.game.crawler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.domain.GameMatch;
import kr.co.victoryfairy.game.domain.GameMatchRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class MatchScheduleSyncServiceTest {

    @Test
    void updatesExistingMatchInPlaceInsteadOfReplacingItsIdentity() {
        GameMatchRepository repository = Mockito.mock(GameMatchRepository.class);
        GameMatch official = match((short) 0, (short) 1, MatchEnum.MatchStatus.END, false);

        new MatchScheduleSyncService(repository).sync(List.of(official));

        ArgumentCaptor<List<GameMatch>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).containsExactly(official);
    }

    private GameMatch match(Short away, Short home, MatchEnum.MatchStatus status, boolean crawled) {
        return new GameMatch("20260819KTLG0", MatchEnum.LeagueType.KBO, null, null, "2026",
                LocalDateTime.of(2026, 8, 19, 19, 0), 1L, "KT", away, 2L, "LG", home, 1L, status, null, crawled, false,
                true, null, null);
    }

}
