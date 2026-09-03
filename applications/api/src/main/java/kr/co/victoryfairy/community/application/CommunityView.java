package kr.co.victoryfairy.community.application;

import java.time.LocalDateTime;
import java.util.List;

import kr.co.victoryfairy.community.domain.CommunityReport;

public interface CommunityView {

    record Cursor<T>(List<T> items, Long nextCursor, boolean hasNext) {
    }

    record Author(Long memberId, String nickname, String profileImageUrl) {
    }

    record Image(Long fileId, String url) {
    }

    record Report(
            Long reportId,
            CommunityReport.TargetType targetType,
            Long targetId,
            Long reporterId,
            Long reportedMemberId,
            CommunityReport.Reason reason,
            CommunityReport.Status status,
            String detail,
            CommunityReport.Snapshot snapshot,
            LocalDateTime createdAt) {
    }

    record PostPreview(
            Long postId,
            String title,
            String content,
            String thumbnailUrl,
            long likeCount,
            long commentCount,
            boolean likedByMe,
            boolean mine,
            LocalDateTime createdAt,
            Author author) {
    }

    record Comment(
            Long commentId,
            String content,
            long likeCount,
            boolean likedByMe,
            boolean mine,
            boolean deleted,
            LocalDateTime createdAt,
            Author author) {
    }

    record PostDetail(
            Long postId,
            String title,
            String content,
            List<Image> images,
            long likeCount,
            long commentCount,
            boolean likedByMe,
            boolean mine,
            LocalDateTime createdAt,
            Author author,
            Cursor<Comment> comments) {
    }

}
