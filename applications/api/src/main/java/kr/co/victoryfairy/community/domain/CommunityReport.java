package kr.co.victoryfairy.community.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record CommunityReport(
        Long id,
        TargetType targetType,
        Long targetId,
        Long reporterId,
        Long reportedMemberId,
        Reason reason,
        Status status,
        String detail,
        Snapshot snapshot,
        LocalDateTime createdAt) {

    public CommunityReport {
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(reporterId, "reporterId");
        Objects.requireNonNull(reportedMemberId, "reportedMemberId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(snapshot, "snapshot");
        detail = detail == null || detail.isBlank() ? null : detail.trim();
    }

    public static CommunityReport forPost(CommunityPost post, Long reporterId, Reason reason, String detail) {
        return new CommunityReport(null, TargetType.POST, post.id(), reporterId, post.memberId(), reason,
                Status.PENDING, detail, new Snapshot(post.id(), post.title(), post.content()), null);
    }

    public static CommunityReport forComment(
            CommunityComment comment, Long reporterId, Reason reason, String detail) {
        return new CommunityReport(null, TargetType.COMMENT, comment.id(), reporterId, comment.memberId(), reason,
                Status.PENDING, detail, new Snapshot(comment.postId(), null, comment.content()), null);
    }

    public enum TargetType {
        POST, COMMENT
    }

    public enum Reason {
        ABUSE, SPAM, INAPPROPRIATE, OTHER
    }

    public enum Status {
        PENDING, ACCEPTED, REJECTED
    }

    public record Snapshot(Long postId, String title, String content) {
    }

}
