package kr.co.victoryfairy.diary.application;

import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.common.model.CommonDto;
import kr.co.victoryfairy.common.service.DiaryFoodDomainService;
import kr.co.victoryfairy.media.application.FileReferenceService;
import kr.co.victoryfairy.common.service.PartnerDomainService;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.storage.db.core.model.DiaryModel;
import kr.co.victoryfairy.storage.db.core.repository.DiaryCustomRepository;
import kr.co.victoryfairy.storage.db.core.repository.DiaryRepository;
import kr.co.victoryfairy.storage.db.core.repository.SeatUseHistoryRepository;
import kr.co.victoryfairy.member.infrastructure.security.MemberAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryListServiceTest {

    @Mock
    private DiaryRepository diaryRepository;
    @Mock
    private DiaryCustomRepository diaryCustomRepository;
    @Mock
    private SeatUseHistoryRepository seatUseHistoryRepository;
    @Mock
    private FileReferenceService fileRefDomainService;
    @Mock
    private DiaryFoodDomainService diaryFoodDomainService;
    @Mock
    private PartnerDomainService partnerDomainService;
    @Mock
    private RedisHandler redisHandler;

    @InjectMocks
    private DiaryQueryService diaryService;

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void returnsEveryDayOfTheMonthForAnUnauthenticatedUser() {
        var responses = diaryService.findList(YearMonth.of(2026, 2));

        assertThat(responses).hasSize(28);
        assertThat(responses.get(0).date()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(responses.get(27).date()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(responses).allSatisfy(response -> {
            assertThat(response.id()).isNull();
            assertThat(response.images()).isEmpty();
        });
        verifyNoInteractions(diaryCustomRepository, fileRefDomainService);
    }

    @Test
    void returnsAnEmptyCalendarWhenTheMemberHasNoDiaries() {
        authenticate(787L);
        var month = YearMonth.of(2026, 8);
        when(diaryCustomRepository.findList(new DiaryModel.ListRequest(787L, month.atDay(1), month.atEndOfMonth())))
            .thenReturn(List.of());

        var responses = diaryService.findList(month);

        assertThat(responses).hasSize(31).allSatisfy(response -> {
            assertThat(response.id()).isNull();
            assertThat(response.teamId()).isNull();
            assertThat(response.image()).isNull();
            assertThat(response.images()).isEmpty();
            assertThat(response.result()).isNull();
        });
        verifyNoInteractions(fileRefDomainService);
    }

    @Test
    void returnsTheLatestDiaryAndAllImagesForEachDay() {
        authenticate(787L);
        var month = YearMonth.of(2026, 8);
        var first = diary(5928L, 13L, LocalDateTime.of(2026, 8, 27, 18, 30),
                LocalDateTime.of(2026, 8, 27, 22, 0), MatchEnum.ResultType.LOSS);
        var latest = diary(5948L, 13L, LocalDateTime.of(2026, 8, 27, 18, 30),
                LocalDateTime.of(2026, 8, 27, 23, 0), MatchEnum.ResultType.WIN);
        when(diaryCustomRepository.findList(new DiaryModel.ListRequest(787L, month.atDay(1), month.atEndOfMonth())))
            .thenReturn(List.of(first, latest));
        when(fileRefDomainService.findImageMapByRefIds(RefType.DIARY, List.of(5928L, 5948L)))
            .thenReturn(Map.of(5928L, image(1L, "first.jpg"), 5948L, image(2L, "latest.jpg")));

        var responses = diaryService.findList(month);

        var gameDay = responses.get(26);
        assertThat(gameDay.date()).isEqualTo(LocalDate.of(2026, 8, 27));
        assertThat(gameDay.id()).isEqualTo(5948L);
        assertThat(gameDay.teamId()).isEqualTo(13L);
        assertThat(gameDay.result()).isEqualTo(MatchEnum.ResultType.WIN);
        assertThat(gameDay.image().id()).isEqualTo(2L);
        assertThat(gameDay.images()).extracting(image -> image.id()).containsExactly(1L, 2L);

        var emptyDay = responses.get(27);
        assertThat(emptyDay.date()).isEqualTo(LocalDate.of(2026, 8, 28));
        assertThat(emptyDay.id()).isNull();
        assertThat(emptyDay.images()).isEmpty();
    }

    @Test
    void returnsDailyDiariesUsingTheRequestedDayBoundaries() {
        authenticate(787L);
        var date = LocalDate.of(2026, 9, 3);
        var start = date.atStartOfDay();
        var endExclusive = date.plusDays(1).atStartOfDay();
        var diary = diary(6000L, 13L, LocalDateTime.of(2026, 9, 3, 18, 30),
                LocalDateTime.of(2026, 9, 3, 22, 0), MatchEnum.ResultType.WIN);
        when(diaryCustomRepository.findDailyList(
                new DiaryModel.DailyListRequest(787L, start, endExclusive)))
            .thenReturn(List.of(diary));
        when(redisHandler.getHashMap("20260903_match_list")).thenReturn(Map.of());
        when(fileRefDomainService.findImageMapByRefIds(RefType.DIARY, List.of(6000L))).thenReturn(Map.of());

        var responses = diaryService.findDailyList(date);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(6000L);
        assertThat(responses.getFirst().date()).isEqualTo(date);
    }

    private void authenticate(Long memberId) {
        var request = new MockHttpServletRequest();
        request.setAttribute("accountByToken", MemberAccount.builder().id(memberId).build());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private DiaryModel.DiaryDto diary(Long id, Long teamId, LocalDateTime matchAt, LocalDateTime createdAt,
            MatchEnum.ResultType result) {
        return new DiaryModel.DiaryDto(id, teamId, "content", matchAt, result, "대전", "대전 한화생명 볼파크",
                MatchEnum.MatchStatus.END, 1L, "원정", (short) 1, 13L, "한화", (short) 2, createdAt);
    }

    private CommonDto.ImageDto image(Long id, String name) {
        return new CommonDto.ImageDto(id, "image/diary/202608", name, "jpg", "https://example.test/" + name);
    }

}
