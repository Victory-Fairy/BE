package kr.co.victoryfairy.game.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.domain.GameMatch;
import kr.co.victoryfairy.game.domain.GameMatchRepository;
import kr.co.victoryfairy.game.domain.GameRecordRepository;
import kr.co.victoryfairy.game.domain.GameUserReader;
import kr.co.victoryfairy.game.domain.Stadium;
import kr.co.victoryfairy.game.domain.StadiumReader;
import kr.co.victoryfairy.game.domain.Team;
import kr.co.victoryfairy.game.domain.TeamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import kr.co.victoryfairy.member.infrastructure.security.MemberAccount;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class GameQueryServiceTest {

    @Mock
    private TeamReader teamRepository;

    @Mock
    private StadiumReader stadiumRepository;

    @Mock
    private GameMatchRepository gameMatchRepository;

    @Mock
    private GameRecordRepository recordRepository;

    @Mock
    private GameUserReader gameUserReader;

    @Mock
    private RedisHandler redisHandler;

    @InjectMocks
    private GameQueryService gameQueryService;

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void loadsMatchRelationsInBatchesAndDoesNotQueryDiariesOneByOneOnCacheMiss() {
        authenticate(787L);
        var date = LocalDate.of(2026, 9, 3);
        var first = match("first", 18, 30);
        var second = match("second", 18, 30);
        when(gameUserReader.preferredTeamId(787L)).thenReturn(Optional.empty());
        when(redisHandler.getHashMap("20260903_match_list")).thenReturn(Map.of());
        when(gameMatchRepository.findByDate(date, MatchEnum.LeagueType.KBO)).thenReturn(List.of(first, second));
        when(teamRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(team(1L, "두산"), team(2L, "LG")));
        when(stadiumRepository.findAllById(Set.of(1L)))
            .thenReturn(List.of(new Stadium(1L, "잠실야구장", "잠실", "잠실", null, true, null, null)));
        when(gameUserReader.diaryIdsByMatchId(787L, List.of("first", "second"))).thenReturn(Map.of("first", 10L));

        var response = gameQueryService.findList(date, MatchEnum.LeagueType.KBO);

        assertThat(response.matchList()).extracting(match -> match.id()).containsExactlyInAnyOrder("first", "second");
        assertThat(response.matchList()).filteredOn(match -> match.id().equals("first")).allSatisfy(match -> {
            assertThat(match.isWrited()).isTrue();
            assertThat(match.diaryId()).isEqualTo(10L);
        });
        verify(teamRepository).findAllById(Set.of(1L, 2L));
        verify(stadiumRepository).findAllById(Set.of(1L));
        verify(gameUserReader).diaryIdsByMatchId(787L, List.of("first", "second"));
    }

    @Test
    void loadsCachedMatchTeamsInOneQueryAndDoesNotQueryDiariesOneByOne() {
        authenticate(787L);
        var date = LocalDate.of(2026, 9, 3);
        when(gameUserReader.preferredTeamId(787L)).thenReturn(Optional.empty());
        when(redisHandler.getHashMap("20260903_match_list"))
            .thenReturn(Map.of("first", cachedMatch(1L, 2L), "second", cachedMatch(1L, 3L)));
        when(teamRepository.findAllById(Set.of(1L, 2L, 3L)))
            .thenReturn(List.of(team(1L, "두산"), team(2L, "LG"), team(3L, "한화")));
        when(gameUserReader.diaryIdsByMatchId(any(), anyCollection())).thenReturn(Map.of());

        var response = gameQueryService.findList(date, MatchEnum.LeagueType.KBO);

        assertThat(response.matchList()).extracting(match -> match.id()).containsExactlyInAnyOrder("first", "second");
        verify(teamRepository).findAllById(Set.of(1L, 2L, 3L));
        verify(gameUserReader).diaryIdsByMatchId(any(), anyCollection());
    }

    @Test
    void databaseMatchListUsesDomainResultIncludingUnknownScores() {
        var date = LocalDate.of(2026, 9, 3);
        var decided = match("decided", 18, 30);
        var unknown = new GameMatch("unknown", MatchEnum.LeagueType.KBO, null, null, "2026",
                LocalDateTime.of(2026, 9, 3, 19, 0), 1L, "두산", (short) 3, 2L, "LG", null, 1L,
                MatchEnum.MatchStatus.READY, null, false, false, true, null, null);
        when(redisHandler.getHashMap("20260903_match_list")).thenReturn(Map.of());
        when(gameMatchRepository.findByDate(date, MatchEnum.LeagueType.KBO)).thenReturn(List.of(decided, unknown));
        when(teamRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(team(1L, "두산"), team(2L, "LG")));
        when(stadiumRepository.findAllById(Set.of(1L)))
            .thenReturn(List.of(new Stadium(1L, "잠실야구장", "잠실", "잠실", null, true, null, null)));

        var response = gameQueryService.findList(date, MatchEnum.LeagueType.KBO);

        assertThat(response.matchList()).filteredOn(match -> match.id().equals("decided")).singleElement()
            .satisfies(match -> {
                assertThat(match.awayTeam().result()).isEqualTo(MatchEnum.ResultType.WIN);
                assertThat(match.homeTeam().result()).isEqualTo(MatchEnum.ResultType.LOSS);
            });
        assertThat(response.matchList()).filteredOn(match -> match.id().equals("unknown")).singleElement()
            .satisfies(match -> {
                assertThat(match.awayTeam().result()).isNull();
                assertThat(match.homeTeam().result()).isNull();
            });
    }

    private void authenticate(Long memberId) {
        var request = new MockHttpServletRequest();
        request.setAttribute("accountByToken", MemberAccount.builder().id(memberId).build());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private GameMatch match(String id, int hour, int minute) {
        return new GameMatch(id, MatchEnum.LeagueType.KBO, null, null, "2026",
                LocalDateTime.of(2026, 9, 3, hour, minute), 1L, "두산", (short) 3, 2L, "LG", (short) 2, 1L,
                MatchEnum.MatchStatus.END, null, false, false, true, null, null);
    }

    private Team team(Long id, String name) {
        return new Team(id, name, name, null, null, null, MatchEnum.LeagueType.KBO, null, true, null, null);
    }

    private Map<String, Object> cachedMatch(Long awayId, Long homeId) {
        return Map.of("league", "KBO", "time", "18:30", "stadium", "잠실", "status", "END", "statusDetail", "종료",
                "awayId", awayId, "homeId", homeId, "awayScore", 3, "homeScore", 2);
    }

}
