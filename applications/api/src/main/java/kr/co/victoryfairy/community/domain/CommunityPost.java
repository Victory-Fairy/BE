package kr.co.victoryfairy.community.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record CommunityPost(
        Long id,
        Long memberId,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime deletedAt) {

    private static final int TITLE_MAX_LENGTH = 30;
    private static final int CONTENT_MAX_LENGTH = 100;

    public CommunityPost {
        Objects.requireNonNull(memberId, "memberId");
        title = normalize(title, TITLE_MAX_LENGTH, "title");
        content = normalize(content, CONTENT_MAX_LENGTH, "content");
    }

    public static CommunityPost create(Long memberId, String title, String content) {
        return new CommunityPost(null, memberId, title, content, null, null);
    }

    public CommunityPost update(String title, String content) {
        return new CommunityPost(id, memberId, title, content, createdAt, deletedAt);
    }

    public CommunityPost delete(LocalDateTime deletedAt) {
        return new CommunityPost(id, memberId, title, content, createdAt,
                Objects.requireNonNull(deletedAt, "deletedAt"));
    }

    public boolean ownedBy(Long memberId) {
        return Objects.equals(this.memberId, memberId);
    }

    private static String normalize(String value, int maxLength, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        var normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return normalized;
    }

}
