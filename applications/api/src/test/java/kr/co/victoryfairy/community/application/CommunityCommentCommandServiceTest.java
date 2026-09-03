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
import org.springframework.http.HttpStatus;

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
        when(repository.updateComment(any())).thenReturn(true);
        var service = new CommunityCommentCommandService(repository);

        service.update(7L, 99L, 31L, " 수정 댓글 ");

        var updated = ArgumentCaptor.forClass(CommunityComment.class);
        verify(repository).updateComment(updated.capture());
        assertThat(updated.getValue().content()).isEqualTo("수정 댓글");
    }

    @Test
    void softDeletesOwnedComment() {
        var repository = mock(CommunityRepository.class);
        when(repository.findActivePost(99L)).thenReturn(Optional.of(post()));
        when(repository.findActiveComment(99L, 31L)).thenReturn(Optional.of(comment(7L)));
        when(repository.deleteComment(any())).thenReturn(true);
        var service = new CommunityCommentCommandService(repository);

        service.delete(7L, 99L, 31L);

        var deleted = ArgumentCaptor.forClass(CommunityComment.class);
        verify(repository).deleteComment(deleted.capture());
        assertThat(deleted.getValue().deletedAt()).isNotNull();
    }

    @Test
    void rejectsDeletingAnotherMembersComment() {
        var repository = mock(CommunityRepository.class);
        when(repository.findActivePost(99L)).thenReturn(Optional.of(post()));
        when(repository.findActiveComment(99L, 31L)).thenReturn(Optional.of(comment(8L)));
        var service = new CommunityCommentCommandService(repository);

        assertThatThrownBy(() -> service.delete(7L, 99L, 31L))
            .isInstanceOfSatisfying(CustomException.class,
                    exception -> assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(repository, never()).deleteComment(any());
    }

    @Test
    void rejectsUpdatingAnotherMembersComment() {
        var repository = mock(CommunityRepository.class);
        when(repository.findActivePost(99L)).thenReturn(Optional.of(post()));
        when(repository.findActiveComment(99L, 31L)).thenReturn(Optional.of(comment(8L)));
        var service = new CommunityCommentCommandService(repository);

        assertThatThrownBy(() -> service.update(7L, 99L, 31L, "수정 댓글"))
            .isInstanceOfSatisfying(CustomException.class,
                    exception -> assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(repository, never()).updateComment(any());
    }

    @Test
    void rejectsWritingToInactivePost() {
        var repository = mock(CommunityRepository.class);
        var service = new CommunityCommentCommandService(repository);

        assertThatThrownBy(() -> service.write(7L, 99L, "댓글")).isInstanceOf(CustomException.class);

        verify(repository, never()).saveComment(any());
    }

    @Test
    void rejectsUpdatingCommentOnInactivePost() {
        var repository = mock(CommunityRepository.class);
        var service = new CommunityCommentCommandService(repository);

        assertThatThrownBy(() -> service.update(7L, 99L, 31L, "수정 댓글"))
            .isInstanceOf(CustomException.class);

        verify(repository, never()).findActiveComment(99L, 31L);
    }

    @Test
    void rejectsDeletingCommentOnInactivePost() {
        var repository = mock(CommunityRepository.class);
        var service = new CommunityCommentCommandService(repository);

        assertThatThrownBy(() -> service.delete(7L, 99L, 31L)).isInstanceOf(CustomException.class);

        verify(repository, never()).findActiveComment(99L, 31L);
    }

    private CommunityPost post() {
        return new CommunityPost(99L, 8L, "제목", "내용", LocalDateTime.of(2026, 9, 3, 12, 0), null);
    }

    private CommunityComment comment(Long memberId) {
        return new CommunityComment(31L, 99L, memberId, "댓글", LocalDateTime.of(2026, 9, 3, 12, 1), null);
    }

}
