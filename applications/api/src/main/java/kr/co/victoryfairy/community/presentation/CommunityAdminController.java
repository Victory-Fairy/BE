package kr.co.victoryfairy.community.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.co.victoryfairy.community.application.CommunityReportAdminService;
import kr.co.victoryfairy.community.application.CommunityView;
import kr.co.victoryfairy.community.domain.CommunityReport;
import kr.co.victoryfairy.web.response.CustomResponse;
import kr.co.victoryfairy.web.response.MessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Community Admin", description = "커뮤니티 신고 관리")
@SecurityRequirement(name = "accessToken")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/community/reports")
public class CommunityAdminController {

    private final CommunityReportAdminService reports;

    @Operation(summary = "신고 목록")
    @GetMapping
    public CustomResponse<CommunityView.Cursor<CommunityView.Report>> findReports(
            @RequestParam CommunityReport.TargetType targetType,
            @RequestParam(required = false) CommunityReport.Status statusCode,
            @RequestParam(required = false) Long cursor) {
        return CustomResponse.ok(reports.findReports(targetType, statusCode, cursor));
    }

    @Operation(summary = "신고 처리")
    @PatchMapping("/{targetType}/{reportId}")
    public CustomResponse<MessageEnum> resolve(
            @PathVariable CommunityReport.TargetType targetType, @PathVariable Long reportId,
            @Valid @RequestBody CommunityApi.ResolveReportRequest request) {
        reports.resolve(targetType, reportId, request.status());
        return CustomResponse.ok(MessageEnum.Common.UPDATE);
    }

}
