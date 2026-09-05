package kr.co.victoryfairy.game.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.victoryfairy.game.domain.MatchEnum;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import kr.co.victoryfairy.member.infrastructure.security.MemberAccount;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.StadiumEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchCustomRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.HitterRecordRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberInfoRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.PitcherRecordRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.StadiumRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.TeamRepository;
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

    @Mock private TeamRepository teamRepository;
    @Mock private StadiumRepository stadiumRepository;
    @Mock private GameMatchRepository gameMatchRepository;
    @Mock private GameMatchCustomRepository gameMatchCustomRepository;
    @Mock private PitcherRecordRepository pitcherRecordRepository;
    @Mock private HitterRecordRepository hitterRecordRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberInfoRepository memberInfoRepository;
    @Mock private DiaryRepository diaryRepository;
    @Mock private RedisHandler redisHandler;

    @InjectMocks private GameQueryService gameQueryService;

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
        when(memberInfoRepository.findByMemberEntity_Id(787L)).thenReturn(Optional.empty());
        when(redisHandler.getHashMap("20260903_match_list")).thenReturn(Map.of());
        when(gameMatchCustomRepository.findByMatchAt(date, MatchEnum.LeagueType.KBO))
            .thenReturn(List.of(first, second));
        when(teamRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(
            new TeamEntity(1L, "두산", "두산"),
            new TeamEntity(2L, "LG", "LG")
        ));
        when(stadiumRepository.findAllById(Set.of(1L))).thenReturn(List.of(
            StadiumEntity.builder().id(1L).shortName("잠실").build()
        ));
        when(diaryRepository.findByMemberIdAndGameMatchEntityIdIn(787L, List.of("first", "second")))
            .thenReturn(List.of(DiaryEntity.builder().id(10L).gameMatchEntity(first).build()));

        var response = gameQueryService.findList(date, MatchEnum.LeagueType.KBO);

        assertThat(response.matchList()).extracting(match -> match.id()).containsExactlyInAnyOrder("first", "second");
        assertThat(response.matchList()).filteredOn(match -> match.id().equals("first"))
            .allSatisfy(match -> {
                assertThat(match.isWrited()).isTrue();
                assertThat(match.diaryId()).isEqualTo(10L);
            });
        verify(teamRepository).findAllById(Set.of(1L, 2L));
        verify(stadiumRepository).findAllById(Set.of(1L));
        verify(diaryRepository).findByMemberIdAndGameMatchEntityIdIn(787L, List.of("first", "second"));
        verify(diaryRepository, never()).findByMemberIdAndGameMatchEntityId(any(), anyString());
    }

    @Test
    void loadsCachedMatchTeamsInOneQueryAndDoesNotQueryDiariesOneByOne() {
        authenticate(787L);
        var date = LocalDate.of(2026, 9, 3);
        when(memberInfoRepository.findByMemberEntity_Id(787L)).thenReturn(Optional.empty());
        when(redisHandler.getHashMap("20260903_match_list")).thenReturn(Map.of(
            "first", cachedMatch(1L, 2L),
            "second", cachedMatch(1L, 3L)
        ));
        when(teamRepository.findAllById(Set.of(1L, 2L, 3L))).thenReturn(List.of(
            new TeamEntity(1L, "두산", "두산"),
            new TeamEntity(2L, "LG", "LG"),
            new TeamEntity(3L, "한화", "한화")
        ));

        var response = gameQueryService.findList(date, MatchEnum.LeagueType.KBO);

        assertThat(response.matchList()).extracting(match -> match.id()).containsExactlyInAnyOrder("first", "second");
        verify(teamRepository).findAllById(Set.of(1L, 2L, 3L));
        verify(diaryRepository, never()).findByMemberIdAndGameMatchEntityId(any(), anyString());
    }

    private void authenticate(Long memberId) {
        var request = new MockHttpServletRequest();
        request.setAttribute("accountByToken", MemberAccount.builder().id(memberId).build());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private GameMatchEntity match(String id, int hour, int minute) {
        return GameMatchEntity.builder()
            .id(id)
            .league(MatchEnum.LeagueType.KBO)
            .matchAt(LocalDateTime.of(2026, 9, 3, hour, minute))
            .awayTeamEntity(new TeamEntity(1L, "두산", "두산"))
            .homeTeamEntity(new TeamEntity(2L, "LG", "LG"))
            .stadiumEntity(StadiumEntity.builder().id(1L).shortName("잠실").build())
            .status(MatchEnum.MatchStatus.END)
            .awayScore((short) 3)
            .homeScore((short) 2)
            .build();
    }

    private Map<String, Object> cachedMatch(Long awayId, Long homeId) {
        return Map.of(
            "league", "KBO",
            "time", "18:30",
            "stadium", "잠실",
            "status", "END",
            "statusDetail", "종료",
            "awayId", awayId,
            "homeId", homeId,
            "awayScore", 3,
            "homeScore", 2
        );
    }
}
