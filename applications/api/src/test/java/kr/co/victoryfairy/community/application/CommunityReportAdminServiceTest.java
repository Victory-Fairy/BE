package kr.co.victoryfairy.community.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.stream.LongStream;

import kr.co.victoryfairy.community.domain.CommunityReport;
import kr.co.victoryfairy.community.domain.CommunityReportRepository;
import kr.co.victoryfairy.web.error.CustomException;
import org.junit.jupiter.api.Test;

class CommunityReportAdminServiceTest {

    @Test
    void returnsTwentyReportsAndNextCursor() {
        var reports = mock(CommunityReportRepository.class);
        var rows = LongStream.rangeClosed(35L, 55L)
            .mapToObj(this::report)
            .sorted((left, right) -> Long.compare(right.id(), left.id()))
            .toList();
        when(reports.find(CommunityReport.TargetType.POST, CommunityReport.Status.PENDING, null, 21))
            .thenReturn(rows);
        var service = new CommunityReportAdminService(reports);

        var result = service.findReports(
                CommunityReport.TargetType.POST, CommunityReport.Status.PENDING, null);

        assertThat(result.items()).hasSize(20);
        assertThat(result.items().getFirst().reportId()).isEqualTo(55L);
        assertThat(result.nextCursor()).isEqualTo(36L);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void returnsNoCursorWhenExactlyTwentyReportsExist() {
        var reports = mock(CommunityReportRepository.class);
        var rows = LongStream.rangeClosed(35L, 54L).mapToObj(this::report).toList();
        when(reports.find(CommunityReport.TargetType.POST, null, null, 21)).thenReturn(rows);
        var service = new CommunityReportAdminService(reports);

        var result = service.findReports(CommunityReport.TargetType.POST, null, null);

        assertThat(result.items()).hasSize(20);
        assertThat(result.nextCursor()).isNull();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void acceptedReportSoftDeletesTarget() {
        var reports = mock(CommunityReportRepository.class);
        when(reports.findById(CommunityReport.TargetType.POST, 55L)).thenReturn(Optional.of(report()));
        when(reports.resolve(CommunityReport.TargetType.POST, 55L, CommunityReport.Status.ACCEPTED)).thenReturn(true);
        var service = new CommunityReportAdminService(reports);

        service.resolve(CommunityReport.TargetType.POST, 55L, CommunityReport.Status.ACCEPTED);

        verify(reports).softDeleteTarget(CommunityReport.TargetType.POST, 99L);
    }

    @Test
    void rejectedReportKeepsTarget() {
        var reports = mock(CommunityReportRepository.class);
        when(reports.findById(CommunityReport.TargetType.POST, 55L)).thenReturn(Optional.of(report()));
        when(reports.resolve(CommunityReport.TargetType.POST, 55L, CommunityReport.Status.REJECTED)).thenReturn(true);
        var service = new CommunityReportAdminService(reports);

        service.resolve(CommunityReport.TargetType.POST, 55L, CommunityReport.Status.REJECTED);

        verify(reports, never()).softDeleteTarget(CommunityReport.TargetType.POST, 99L);
    }

    @Test
    void rejectsPendingAsResolution() {
        var reports = mock(CommunityReportRepository.class);
        var service = new CommunityReportAdminService(reports);

        assertThatThrownBy(() -> service.resolve(
                CommunityReport.TargetType.POST, 55L, CommunityReport.Status.PENDING))
            .isInstanceOf(CustomException.class);
    }

    @Test
    void rejectsMissingReport() {
        var reports = mock(CommunityReportRepository.class);
        var service = new CommunityReportAdminService(reports);

        assertThatThrownBy(() -> service.resolve(
                CommunityReport.TargetType.POST, 55L, CommunityReport.Status.ACCEPTED))
            .isInstanceOf(CustomException.class);
    }

    private CommunityReport report() {
        return report(55L);
    }

    private CommunityReport report(Long id) {
        return new CommunityReport(id, CommunityReport.TargetType.POST, 99L, 7L, 8L,
                CommunityReport.Reason.SPAM, CommunityReport.Status.PENDING, null,
                new CommunityReport.Snapshot(99L, "제목", "내용"), LocalDateTime.of(2026, 9, 3, 12, 2));
    }

}
