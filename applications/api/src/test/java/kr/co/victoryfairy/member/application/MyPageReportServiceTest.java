package kr.co.victoryfairy.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import kr.co.victoryfairy.diary.domain.DiaryEnum;
import kr.co.victoryfairy.diary.domain.GameRecord;
import kr.co.victoryfairy.diary.domain.GameRecordStore;
import kr.co.victoryfairy.member.domain.MemberStore;
import kr.co.victoryfairy.member.domain.MemberQueryStore;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.media.infrastructure.S3PresignedUrlService;
import kr.co.victoryfairy.member.infrastructure.security.MemberAccount;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.web.response.MessageEnum;
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
class MyPageReportServiceTest {

    @Mock
    private MemberStore memberRepository;

    @Mock
    private MemberQueryStore memberCustomRepository;

    @Mock
    private GameRecordStore gameRecordRepository;

    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @InjectMocks
    private MyPageQueryService service;

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void calculatesTheSameReportFromRecordsLoadedByMemberIdAndSeason() {
        authenticate(787L);
        var matchAt = LocalDateTime.of(2026, 8, 27, 18, 30);
        var record = new GameRecord(1L, 787L, 6000L, "20260827HHSK0", 13L, "한화", 4L, "삼성", 1L,
                "대전 한화생명 볼파크", DiaryEnum.ViewType.STADIUM, MatchEnum.MatchStatus.END,
                MatchEnum.ResultType.WIN, "2026", MatchEnum.LeagueType.KBO, matchAt, 13L, null, null);
        when(gameRecordRepository.findByMemberAndSeasonOrdered(787L, "2026"))
            .thenReturn(List.of(record));

        var result = service.findReport("2026");

        assertThat(result.stadium().winAvg()).isEqualTo((short) 100);
        assertThat(result.stadium().win()).isEqualTo((short) 1);
        assertThat(result.home()).isNull();
        assertThat(result.viewStatistics().winTeam()).isEqualTo("삼성");
        assertThat(result.viewStatistics().lossTeam()).isEqualTo("-");
        assertThat(result.viewStatistics().stadium()).isEqualTo("대전 한화생명 볼파크");
        assertThat(result.viewStatistics().winningStreak()).isEqualTo((short) 1);
        assertThat(result.viewStatistics().homeWinAvg()).isEqualTo((short) 100);
        assertThat(result.viewStatistics().stadiumWinAvg()).isZero();
    }

    @Test
    void rejectsAReportRequestWhenTheAuthenticatedMemberNoLongerExists() {
        authenticate(787L);
        when(gameRecordRepository.findByMemberAndSeasonOrdered(787L, "2026"))
            .thenReturn(List.of());
        when(memberRepository.memberExists(787L)).thenReturn(false);

        assertThatThrownBy(() -> service.findReport("2026"))
            .isInstanceOf(CustomException.class)
            .hasMessage(MessageEnum.Data.FAIL_NO_RESULT.getDescKr());
    }

    private void authenticate(Long memberId) {
        var request = new MockHttpServletRequest();
        request.setAttribute("accountByToken", MemberAccount.builder().id(memberId).build());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

}
