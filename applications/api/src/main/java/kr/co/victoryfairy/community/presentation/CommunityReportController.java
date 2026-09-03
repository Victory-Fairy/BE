package kr.co.victoryfairy.community.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.co.victoryfairy.community.application.CommunityReportCommandService;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import kr.co.victoryfairy.web.response.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Community", description = "커뮤니티")
@SecurityRequirement(name = "accessToken")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/posts")
public class CommunityReportController {

    private final CommunityReportCommandService reports;

    @Operation(summary = "게시글 신고")
    @PostMapping("/{postId}/reports")
    public CustomResponse<CommunityApi.ReportResponse> reportPost(
            @PathVariable Long postId, @Valid @RequestBody CommunityApi.ReportRequest request) {
        var reportId = reports.reportPost(CurrentRequest.getId(), postId, request.reason(), request.detail());
        return CustomResponse.ok(new CommunityApi.ReportResponse(reportId));
    }

    @Operation(summary = "댓글 신고")
    @PostMapping("/{postId}/comments/{commentId}/reports")
    public CustomResponse<CommunityApi.ReportResponse> reportComment(
            @PathVariable Long postId, @PathVariable Long commentId,
            @Valid @RequestBody CommunityApi.ReportRequest request) {
        var reportId = reports.reportComment(
                CurrentRequest.getId(), postId, commentId, request.reason(), request.detail());
        return CustomResponse.ok(new CommunityApi.ReportResponse(reportId));
    }

}
