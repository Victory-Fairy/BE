package kr.co.victoryfairy.diary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import kr.co.victoryfairy.diary.domain.DiaryEnum;
import kr.co.victoryfairy.diary.domain.Diary;
import kr.co.victoryfairy.diary.domain.DiaryStore;
import kr.co.victoryfairy.diary.domain.DiaryQueryStore;
import kr.co.victoryfairy.diary.domain.SeatUseStore;
import kr.co.victoryfairy.game.domain.GameMatch;
import kr.co.victoryfairy.game.domain.GameMatchRepository;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.diary.application.DiaryFoodDomainService;
import kr.co.victoryfairy.diary.application.PartnerDomainService;
import kr.co.victoryfairy.media.application.FileReferenceService;
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
class DiaryDetailServiceTest {

    @Mock
    private DiaryStore diaryRepository;

    @Mock
    private DiaryQueryStore diaryCustomRepository;

    @Mock
    private SeatUseStore seatUseHistoryRepository;

    @Mock
    private GameMatchRepository matches;

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
        var match = new GameMatch("20260903HHLT0", MatchEnum.LeagueType.KBO, null, null, "2026",
                LocalDateTime.of(2026, 9, 3, 18, 30), 4L, "삼성", (short) 1, 13L, "한화", (short) 3,
                1L, MatchEnum.MatchStatus.END, null, false, false, true, null, null);
        var diary = new Diary(6000L, 787L, match.id(), 13L, "한화", DiaryEnum.ViewType.HOME, null, null,
                "승리 일기", false, null, null);
        when(diaryRepository.findDetailByMemberAndId(787L, 6000L)).thenReturn(Optional.of(diary));
        when(matches.findById(match.id())).thenReturn(Optional.of(match));
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
