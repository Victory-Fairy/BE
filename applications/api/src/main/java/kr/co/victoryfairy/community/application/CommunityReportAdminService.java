package kr.co.victoryfairy.community.application;

import kr.co.victoryfairy.community.domain.CommunityReport;
import kr.co.victoryfairy.community.domain.CommunityReportRepository;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.web.response.MessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityReportAdminService {

    private static final int PAGE_SIZE = 20;

    private final CommunityReportRepository reports;

    @Transactional(readOnly = true)
    public CommunityView.Cursor<CommunityView.Report> findReports(
            CommunityReport.TargetType targetType, CommunityReport.Status status, Long cursor) {
        var fetched = reports.find(targetType, status, cursor, PAGE_SIZE + 1);
        var hasNext = fetched.size() > PAGE_SIZE;
        var page = fetched.stream().limit(PAGE_SIZE).toList();
        var items = page.stream()
            .map(report -> new CommunityView.Report(
                report.id(), report.targetType(), report.targetId(), report.reporterId(), report.reportedMemberId(),
                report.reason(), report.status(), report.detail(), report.snapshot(), report.createdAt()))
            .toList();
        return new CommunityView.Cursor<>(items, hasNext ? page.getLast().id() : null, hasNext);
    }

    @Transactional
    public void resolve(
            CommunityReport.TargetType targetType, Long reportId, CommunityReport.Status status) {
        if (status == CommunityReport.Status.PENDING) {
            throw new CustomException(MessageEnum.Data.WRONG_APPROACH);
        }

        var report = reports.findById(targetType, reportId)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));
        if (!reports.resolve(targetType, reportId, status)) {
            throw new CustomException(MessageEnum.Data.WRONG_APPROACH);
        }
        if (status == CommunityReport.Status.ACCEPTED) {
            reports.softDeleteTarget(targetType, report.targetId());
        }
    }

}
