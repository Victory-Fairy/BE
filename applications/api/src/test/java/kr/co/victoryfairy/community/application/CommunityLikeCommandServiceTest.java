package kr.co.victoryfairy.community.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import kr.co.victoryfairy.community.domain.CommunityComment;
import kr.co.victoryfairy.community.domain.CommunityPost;
import kr.co.victoryfairy.community.domain.CommunityRepository;
import kr.co.victoryfairy.web.error.CustomException;
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

    @Test
    void rejectsPostLikeForInactivePost() {
        var repository = mock(CommunityRepository.class);
        var service = new CommunityLikeCommandService(repository);

        assertThatThrownBy(() -> service.setPostLike(7L, 99L, true)).isInstanceOf(CustomException.class);

        verify(repository, never()).setPostLike(99L, 7L, true);
    }

    @Test
    void rejectsCommentLikeForInactivePost() {
        var repository = mock(CommunityRepository.class);
        var service = new CommunityLikeCommandService(repository);

        assertThatThrownBy(() -> service.setCommentLike(7L, 99L, 31L, true))
            .isInstanceOf(CustomException.class);

        verify(repository, never()).findActiveComment(99L, 31L);
    }

    private CommunityPost post() {
        return new CommunityPost(99L, 8L, "제목", "내용", LocalDateTime.of(2026, 9, 3, 12, 0), null);
    }

    private CommunityComment comment() {
        return new CommunityComment(31L, 99L, 8L, "댓글", LocalDateTime.of(2026, 9, 3, 12, 1), null);
    }

}
