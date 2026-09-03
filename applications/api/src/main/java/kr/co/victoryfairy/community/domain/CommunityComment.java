package kr.co.victoryfairy.community.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record CommunityComment(
        Long id,
        Long postId,
        Long memberId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime deletedAt) {

    private static final int CONTENT_MAX_LENGTH = 100;

    public CommunityComment {
        Objects.requireNonNull(postId, "postId");
        Objects.requireNonNull(memberId, "memberId");
        content = normalize(content);
    }

    public static CommunityComment create(Long postId, Long memberId, String content) {
        return new CommunityComment(null, postId, memberId, content, null, null);
    }

    public CommunityComment update(String content) {
        return new CommunityComment(id, postId, memberId, content, createdAt, deletedAt);
    }

    public CommunityComment delete(LocalDateTime deletedAt) {
        return new CommunityComment(id, postId, memberId, content, createdAt,
                Objects.requireNonNull(deletedAt, "deletedAt"));
    }

    public boolean ownedBy(Long memberId) {
        return Objects.equals(this.memberId, memberId);
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        var normalized = value.trim();
        if (normalized.length() > CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException("content is too long");
        }
        return normalized;
    }
}
