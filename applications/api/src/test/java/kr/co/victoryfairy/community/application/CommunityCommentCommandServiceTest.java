package kr.co.victoryfairy.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.ArgumentCaptor;

class CommunityCommentCommandServiceTest {

    @Test
    void writesCommentToActivePost() {
        var repository = mock(CommunityRepository.class);
        when(repository.findActivePost(99L)).thenReturn(Optional.of(post()));
        when(repository.saveComment(any())).thenReturn(
            new CommunityComment(31L, 99L, 7L, "댓글", LocalDateTime.of(2026, 9, 3, 12, 1), null));
        var service = new CommunityCommentCommandService(repository);

        var commentId = service.write(7L, 99L, " 댓글 ");

        assertThat(commentId).isEqualTo(31L);
    }

    @Test
    void updatesOwnedComment() {
        var repository = mock(CommunityRepository.class);
        when(repository.findActivePost(99L)).thenReturn(Optional.of(post()));
        when(repository.findActiveComment(99L, 31L)).thenReturn(Optional.of(comment(7L)));
        var service = new CommunityCommentCommandService(repository);

        service.update(7L, 99L, 31L, " 수정 댓글 ");

        var updated = ArgumentCaptor.forClass(CommunityComment.class);
        verify(repository).saveComment(updated.capture());
        assertThat(updated.getValue().content()).isEqualTo("수정 댓글");
    }

    @Test
    void rejectsDeletingAnotherMembersComment() {
        var repository = mock(CommunityRepository.class);
        when(repository.findActivePost(99L)).thenReturn(Optional.of(post()));
        when(repository.findActiveComment(99L, 31L)).thenReturn(Optional.of(comment(8L)));
        var service = new CommunityCommentCommandService(repository);

        assertThatThrownBy(() -> service.delete(7L, 99L, 31L)).isInstanceOf(CustomException.class);
        verify(repository, never()).saveComment(any());
    }

    private CommunityPost post() {
        return new CommunityPost(99L, 8L, "제목", "내용", LocalDateTime.of(2026, 9, 3, 12, 0), null);
    }

    private CommunityComment comment(Long memberId) {
        return new CommunityComment(31L, 99L, memberId, "댓글", LocalDateTime.of(2026, 9, 3, 12, 1), null);
    }

}
