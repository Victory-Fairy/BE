package kr.co.victoryfairy.community.domain;

import java.util.List;
import java.util.Optional;

public interface CommunityReportRepository {

    Optional<Long> save(CommunityReport report);

    List<CommunityReport> find(
            CommunityReport.TargetType targetType, CommunityReport.Status status, Long cursor, int limit);

    Optional<CommunityReport> findById(CommunityReport.TargetType targetType, Long reportId);

    boolean resolve(
            CommunityReport.TargetType targetType, Long reportId, CommunityReport.Status status);

    void softDeleteTarget(CommunityReport.TargetType targetType, Long targetId);

}
