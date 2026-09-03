package kr.co.victoryfairy.community.application;

import kr.co.victoryfairy.community.domain.CommunityReport;
import kr.co.victoryfairy.community.domain.CommunityReportRepository;
import kr.co.victoryfairy.community.domain.CommunityRepository;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.web.response.MessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityReportCommandService {

    private final CommunityRepository community;

    private final CommunityReportRepository reports;

    @Transactional
    public Long reportPost(Long reporterId, Long postId, CommunityReport.Reason reason, String detail) {
        var post = community.findActivePost(postId).orElseThrow(this::noResult);
        return save(CommunityReport.forPost(post, reporterId, reason, detail));
    }

    @Transactional
    public Long reportComment(
            Long reporterId, Long postId, Long commentId, CommunityReport.Reason reason, String detail) {
        community.findActivePost(postId).orElseThrow(this::noResult);
        var comment = community.findActiveComment(postId, commentId).orElseThrow(this::noResult);
        return save(CommunityReport.forComment(comment, reporterId, reason, detail));
    }

    private Long save(CommunityReport report) {
        return reports.save(report)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_DUPLICATE));
    }

    private CustomException noResult() {
        return new CustomException(MessageEnum.Data.FAIL_NO_RESULT);
    }

}
