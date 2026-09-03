package kr.co.victoryfairy.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CommunityReportTest {

    @Test
    void capturesPostSnapshotAtReportTime() {
        var post = new CommunityPost(99L, 8L, "제목", "내용", LocalDateTime.of(2026, 9, 3, 12, 0), null);

        var report = CommunityReport.forPost(post, 7L, CommunityReport.Reason.SPAM, "  반복 게시  ");

        assertThat(report.targetType()).isEqualTo(CommunityReport.TargetType.POST);
        assertThat(report.targetId()).isEqualTo(99L);
        assertThat(report.reportedMemberId()).isEqualTo(8L);
        assertThat(report.status()).isEqualTo(CommunityReport.Status.PENDING);
        assertThat(report.detail()).isEqualTo("반복 게시");
        assertThat(report.snapshot()).isEqualTo(new CommunityReport.Snapshot(99L, "제목", "내용"));
    }

    @Test
    void capturesCommentSnapshotAtReportTime() {
        var comment = new CommunityComment(31L, 99L, 8L, "댓글", LocalDateTime.of(2026, 9, 3, 12, 1), null);

        var report = CommunityReport.forComment(comment, 7L, CommunityReport.Reason.ABUSE, null);

        assertThat(report.targetType()).isEqualTo(CommunityReport.TargetType.COMMENT);
        assertThat(report.targetId()).isEqualTo(31L);
        assertThat(report.snapshot()).isEqualTo(new CommunityReport.Snapshot(99L, null, "댓글"));
    }

    @Test
    void rejectsMissingRequiredReportData() {
        assertThatNullPointerException().isThrownBy(() -> new CommunityReport(
                null, null, 99L, 7L, 8L, CommunityReport.Reason.SPAM, CommunityReport.Status.PENDING,
                null, new CommunityReport.Snapshot(99L, "제목", "내용"), null));
    }

}
