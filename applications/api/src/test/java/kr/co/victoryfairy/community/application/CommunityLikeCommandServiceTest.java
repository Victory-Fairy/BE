package kr.co.victoryfairy.community.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import kr.co.victoryfairy.community.domain.CommunityComment;
import kr.co.victoryfairy.community.domain.CommunityPost;
import kr.co.victoryfairy.community.domain.CommunityRepository;
import org.junit.jupiter.api.Test;

class CommunityLikeCommandServiceTest {

    @Test
    void setsPostLikeToRequestedState() {
        var repository = mock(CommunityRepository.class);
        when(repository.findActivePost(99L)).thenReturn(Optional.of(post()));
        var service = new CommunityLikeCommandService(repository);

        service.setPostLike(7L, 99L, true);

        verify(repository).setPostLike(99L, 7L, true);
    }

    @Test
    void setsCommentLikeToRequestedState() {
        var repository = mock(CommunityRepository.class);
        when(repository.findActivePost(99L)).thenReturn(Optional.of(post()));
        when(repository.findActiveComment(99L, 31L)).thenReturn(Optional.of(comment()));
        var service = new CommunityLikeCommandService(repository);

        service.setCommentLike(7L, 99L, 31L, false);

        verify(repository).setCommentLike(31L, 7L, false);
    }

    private CommunityPost post() {
        return new CommunityPost(99L, 8L, "제목", "내용", LocalDateTime.of(2026, 9, 3, 12, 0), null);
    }

    private CommunityComment comment() {
        return new CommunityComment(31L, 99L, 8L, "댓글", LocalDateTime.of(2026, 9, 3, 12, 1), null);
    }

}
