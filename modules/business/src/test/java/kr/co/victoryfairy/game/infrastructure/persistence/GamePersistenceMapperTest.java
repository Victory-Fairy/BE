package kr.co.victoryfairy.game.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.StadiumEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;
import org.junit.jupiter.api.Test;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchCustomRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.TeamRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.StadiumRepository;

class GamePersistenceMapperTest {

    @Test
    void mapsOnlyAssociationIdsAndPreservesNullableAuditValues() {
        var away = mock(TeamEntity.class);
        var home = mock(TeamEntity.class);
        var stadium = mock(StadiumEntity.class);
        var entity = mock(GameMatchEntity.class);
        var created = LocalDateTime.of(2026, 1, 1, 0, 0);
        when(away.getId()).thenReturn(1L);
        when(home.getId()).thenReturn(2L);
        when(stadium.getId()).thenReturn(3L);
        when(entity.getId()).thenReturn("match");
        when(entity.getLeague()).thenReturn(MatchEnum.LeagueType.KBO);
        when(entity.getAwayTeamEntity()).thenReturn(away);
        when(entity.getHomeTeamEntity()).thenReturn(home);
        when(entity.getStadiumEntity()).thenReturn(stadium);
        when(entity.getCreatedAt()).thenReturn(created);
        when(entity.getIsUse()).thenReturn(true);
        when(entity.getAwayScore()).thenReturn(null);
        when(entity.getHomeScore()).thenReturn(null);

        var match = GamePersistenceMapper.toDomain(entity);

        assertThat(match.awayTeamId()).isEqualTo(1L);
        assertThat(match.homeTeamId()).isEqualTo(2L);
        assertThat(match.stadiumId()).isEqualTo(3L);
        assertThat(match.awayScore()).isNull();
        assertThat(match.updatedAt()).isNull();
        assertThat(match.createdAt()).isEqualTo(created);
        verify(away, never()).getName();
        verify(home, never()).getName();
        verify(stadium, never()).getShortName();
    }

    @Test
    void batchSaveUpdatesExistingRowsAndKeepsAuditAndCrawlerState() {
        var rows = mock(kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchRepository.class);
        var existing = GameMatchEntity.builder()
            .id("match")
            .league(MatchEnum.LeagueType.KBO)
            .status(MatchEnum.MatchStatus.READY)
            .isMatchInfoCraw(true)
            .isSendPush(true)
            .build();
        var createdAt = existing.getCreatedAt();
        var official = new kr.co.victoryfairy.game.domain.GameMatch("match", MatchEnum.LeagueType.KBO, null, null,
                "2026", LocalDateTime.of(2026, 9, 5, 18, 30), null, "A", (short) 1, null, "B", (short) 2, null,
                MatchEnum.MatchStatus.END, null, false, false, true, null, null);
        when(rows.findAllById(List.of("match"))).thenReturn(List.of(existing));
        when(rows.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        var adapter = new GamePersistenceAdapter(rows, mock(GameMatchCustomRepository.class),
                mock(TeamRepository.class), mock(StadiumRepository.class));

        var saved = adapter.saveAll(List.of(official)).getFirst();

        assertThat(saved.id()).isEqualTo("match");
        assertThat(saved.status()).isEqualTo(MatchEnum.MatchStatus.END);
        assertThat(saved.detailCrawled()).isTrue();
        assertThat(saved.pushSent()).isTrue();
        assertThat(saved.createdAt()).isEqualTo(createdAt);
        verify(rows, never()).findById(any());
    }

    @Test
    void dateOnlyReadUsesTheOriginalUnfilteredQuery() {
        var rows = mock(kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchRepository.class);
        var queries = mock(GameMatchCustomRepository.class);
        var adapter = new GamePersistenceAdapter(rows, queries, mock(TeamRepository.class),
                mock(StadiumRepository.class));
        var date = LocalDate.of(2026, 9, 5);

        adapter.findByDate(date);

        verify(queries).findByMatchAt(date);
        verify(queries, never()).findByMatchAt(date, null);
    }

}
