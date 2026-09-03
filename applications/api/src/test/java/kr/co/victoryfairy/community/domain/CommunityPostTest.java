package kr.co.victoryfairy.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CommunityPostTest {

    @Test
    void createsPostFromMemberIdAndNormalizesText() {
        var post = CommunityPost.create(7L, "  제목  ", "  내용  ");

        assertThat(post.memberId()).isEqualTo(7L);
        assertThat(post.title()).isEqualTo("제목");
        assertThat(post.content()).isEqualTo("내용");
    }

    @Test
    void rejectsMissingMemberId() {
        assertThatNullPointerException()
            .isThrownBy(() -> CommunityPost.create(null, "제목", "내용"));
    }

    @Test
    void rejectsBlankNormalizedText() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> CommunityPost.create(7L, "  ", "내용"));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> CommunityPost.create(7L, "제목", "  "));
    }

    @Test
    void rejectsTextOverApiLimits() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> CommunityPost.create(7L, "가".repeat(31), "내용"));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> CommunityPost.create(7L, "제목", "가".repeat(101)));
    }

    @Test
    void updatesContentWithoutChangingIdentityOrAuthor() {
        var post = new CommunityPost(99L, 7L, "이전 제목", "이전 내용",
                LocalDateTime.of(2026, 9, 3, 12, 0), null);

        var updated = post.update(" 새 제목 ", " 새 내용 ");

        assertThat(updated.id()).isEqualTo(99L);
        assertThat(updated.memberId()).isEqualTo(7L);
        assertThat(updated.title()).isEqualTo("새 제목");
        assertThat(updated.content()).isEqualTo("새 내용");
    }

    @Test
    void marksPostDeletedAtGivenTime() {
        var post = CommunityPost.create(7L, "제목", "내용");
        var deletedAt = LocalDateTime.of(2026, 9, 3, 13, 0);

        var deleted = post.delete(deletedAt);

        assertThat(deleted.deletedAt()).isEqualTo(deletedAt);
        assertThat(deleted.ownedBy(7L)).isTrue();
        assertThat(deleted.ownedBy(8L)).isFalse();
    }

}
