package kr.co.victoryfairy.community.presentation;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.co.victoryfairy.community.domain.CommunityReport;

public interface CommunityApi {

    record WriteRequest(
            @NotBlank @Size(max = 30) String title,
            @NotBlank @Size(max = 100) String content,
            List<Long> fileIds) {
    }

    record WriteResponse(Long postId) {
    }

    record CommentRequest(@NotBlank @Size(max = 100) String content) {
    }

    record WriteCommentResponse(Long commentId) {
    }

    record LikeRequest(@NotNull Boolean liked) {
    }

    record ReportRequest(
            @NotNull CommunityReport.Reason reason,
            @Size(max = 500) String detail) {
    }

    record ReportResponse(Long reportId) {
    }

    record ResolveReportRequest(@NotNull CommunityReport.Status status) {
    }

}
