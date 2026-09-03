package kr.co.victoryfairy.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CommunityCommentTest {

    @Test
    void createsAndNormalizesCommentFromIds() {
        var comment = CommunityComment.create(99L, 7L, "  댓글  ");

        assertThat(comment.postId()).isEqualTo(99L);
        assertThat(comment.memberId()).isEqualTo(7L);
        assertThat(comment.content()).isEqualTo("댓글");
    }

    @Test
    void updatesAndSoftDeletesComment() {
        var comment = CommunityComment.create(99L, 7L, "댓글");
        var deletedAt = LocalDateTime.of(2026, 9, 3, 13, 0);

        var updated = comment.update(" 수정 댓글 ");
        var deleted = updated.delete(deletedAt);

        assertThat(updated.content()).isEqualTo("수정 댓글");
        assertThat(deleted.deletedAt()).isEqualTo(deletedAt);
        assertThat(deleted.ownedBy(7L)).isTrue();
        assertThat(deleted.ownedBy(8L)).isFalse();
    }

    @Test
    void rejectsInvalidCommentContent() {
        assertThat(CommunityComment.create(99L, 7L, "가".repeat(100)).content())
            .hasSize(100);
        assertThatIllegalArgumentException()
            .isThrownBy(() -> CommunityComment.create(99L, 7L, "  "));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> CommunityComment.create(99L, 7L, "가".repeat(101)));
    }

}
