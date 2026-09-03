package kr.co.victoryfairy.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

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

}
