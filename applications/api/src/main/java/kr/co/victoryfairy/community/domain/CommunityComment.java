package kr.co.victoryfairy.community.domain;

import java.time.LocalDateTime;

public record CommunityComment(
        Long id,
        Long postId,
        Long memberId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime deletedAt) {
}
