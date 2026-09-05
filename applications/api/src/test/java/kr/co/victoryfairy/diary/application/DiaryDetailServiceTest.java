package kr.co.victoryfairy.diary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import kr.co.victoryfairy.diary.domain.DiaryEnum;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.diary.application.DiaryFoodDomainService;
import kr.co.victoryfairy.diary.application.PartnerDomainService;
import kr.co.victoryfairy.media.application.FileReferenceService;
import kr.co.victoryfairy.member.infrastructure.security.MemberAccount;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.TeamEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryCustomRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatUseHistoryRepository;
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
class DiaryDetailServiceTest {

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private DiaryCustomRepository diaryCustomRepository;

    @Mock
    private SeatUseHistoryRepository seatUseHistoryRepository;

    @Mock
    private FileReferenceService fileReferenceService;

    @Mock
    private DiaryFoodDomainService diaryFoodService;

    @Mock
    private PartnerDomainService partnerService;

    @Mock
    private RedisHandler redisHandler;

    @InjectMocks
    private DiaryQueryService service;

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void returnsDiaryDetailFromTheDetailFetchPath() {
        authenticate(787L);
        var myTeam = new TeamEntity(13L, "한화", "한화");
        var opponent = new TeamEntity(4L, "삼성", "삼성");
        var match = GameMatchEntity.builder()
            .id("20260903HHLT0")
            .league(MatchEnum.LeagueType.KBO)
            .matchAt(LocalDateTime.of(2026, 9, 3, 18, 30))
            .awayTeamEntity(opponent)
            .homeTeamEntity(myTeam)
            .awayScore((short) 1)
            .homeScore((short) 3)
            .status(MatchEnum.MatchStatus.END)
            .build();
        var diary = DiaryEntity.builder()
            .id(6000L)
            .gameMatchEntity(match)
            .teamEntity(myTeam)
            .viewType(DiaryEnum.ViewType.HOME)
            .content("승리 일기")
            .build();
        when(diaryRepository.findDetailByMemberIdAndId(787L, 6000L)).thenReturn(Optional.of(diary));
        when(diaryFoodService.findFoodNamesByRefId(RefType.DIARY, 6000L)).thenReturn(List.of());
        when(fileReferenceService.findImagesByRefId(RefType.DIARY, 6000L)).thenReturn(List.of());
        when(partnerService.findPartnersByRefId(RefType.DIARY, 6000L)).thenReturn(List.of());

        var result = service.findById(6000L);

        assertThat(result.teamId()).isEqualTo(13L);
        assertThat(result.gameMatchId()).isEqualTo("20260903HHLT0");
        assertThat(result.content()).isEqualTo("승리 일기");
        assertThat(result.result()).isEqualTo(MatchEnum.ResultType.WIN);
        assertThat(result.leagueType()).isEqualTo(MatchEnum.LeagueType.KBO);
    }

    private void authenticate(Long memberId) {
        var request = new MockHttpServletRequest();
        request.setAttribute("accountByToken", MemberAccount.builder().id(memberId).build());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

}
