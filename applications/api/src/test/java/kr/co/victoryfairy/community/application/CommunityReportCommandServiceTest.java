package kr.co.victoryfairy.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import kr.co.victoryfairy.community.domain.CommunityComment;
import kr.co.victoryfairy.community.domain.CommunityPost;
import kr.co.victoryfairy.community.domain.CommunityReport;
import kr.co.victoryfairy.community.domain.CommunityReportRepository;
import kr.co.victoryfairy.community.domain.CommunityRepository;
import kr.co.victoryfairy.web.error.CustomException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CommunityReportCommandServiceTest {

    @Test
    void reportsActivePostWithSnapshot() {
        var community = mock(CommunityRepository.class);
        var reports = mock(CommunityReportRepository.class);
        when(community.findActivePost(99L)).thenReturn(Optional.of(post()));
        when(reports.save(any())).thenReturn(Optional.of(55L));
        var service = new CommunityReportCommandService(community, reports);

        var reportId = service.reportPost(7L, 99L, CommunityReport.Reason.SPAM, "반복 게시");

        var report = ArgumentCaptor.forClass(CommunityReport.class);
        verify(reports).save(report.capture());
        assertThat(report.getValue().snapshot().content()).isEqualTo("내용");
        assertThat(reportId).isEqualTo(55L);
    }

    @Test
    void rejectsDuplicatePostReport() {
        var community = mock(CommunityRepository.class);
        var reports = mock(CommunityReportRepository.class);
        when(community.findActivePost(99L)).thenReturn(Optional.of(post()));
        when(reports.save(any())).thenReturn(Optional.empty());
        var service = new CommunityReportCommandService(community, reports);

        assertThatThrownBy(() -> service.reportPost(7L, 99L, CommunityReport.Reason.SPAM, null))
            .isInstanceOf(CustomException.class);
    }

    @Test
    void rejectsReportForMissingPost() {
        var community = mock(CommunityRepository.class);
        var service = new CommunityReportCommandService(community, mock(CommunityReportRepository.class));

        assertThatThrownBy(() -> service.reportPost(7L, 99L, CommunityReport.Reason.SPAM, null))
            .isInstanceOf(CustomException.class);
    }

    @Test
    void reportsCommentBelongingToActivePost() {
        var community = mock(CommunityRepository.class);
        var reports = mock(CommunityReportRepository.class);
        when(community.findActivePost(99L)).thenReturn(Optional.of(post()));
        when(community.findActiveComment(99L, 31L)).thenReturn(Optional.of(comment()));
        when(reports.save(any())).thenReturn(Optional.of(56L));
        var service = new CommunityReportCommandService(community, reports);

        var reportId = service.reportComment(7L, 99L, 31L, CommunityReport.Reason.ABUSE, null);

        assertThat(reportId).isEqualTo(56L);
    }

    private CommunityPost post() {
        return new CommunityPost(99L, 8L, "제목", "내용", LocalDateTime.of(2026, 9, 3, 12, 0), null);
    }

    private CommunityComment comment() {
        return new CommunityComment(31L, 99L, 8L, "댓글", LocalDateTime.of(2026, 9, 3, 12, 1), null);
    }

}
