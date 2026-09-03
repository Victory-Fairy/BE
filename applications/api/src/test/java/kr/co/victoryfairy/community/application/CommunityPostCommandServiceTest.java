package kr.co.victoryfairy.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import kr.co.victoryfairy.community.domain.CommunityPost;
import kr.co.victoryfairy.community.domain.CommunityRepository;
import kr.co.victoryfairy.web.error.CustomException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CommunityPostCommandServiceTest {

    @Test
    void writesNormalizedPostAndKeepsUniqueFileOrder() {
        var repository = mock(CommunityRepository.class);
        var members = mock(CommunityMemberReader.class);
        var files = mock(CommunityFileReader.class);
        when(members.exists(7L)).thenReturn(true);
        when(files.findExistingIds(List.of(20L, 10L))).thenReturn(Set.of(10L, 20L));
        when(repository.save(any())).thenReturn(new CommunityPost(99L, 7L, "제목", "내용", null, null));
        var service = new CommunityPostCommandService(repository, members, files);

        var postId = service.write(7L, " 제목 ", " 내용 ", List.of(20L, 10L, 20L));

        var post = ArgumentCaptor.forClass(CommunityPost.class);
        verify(repository).save(post.capture());
        assertThat(post.getValue().memberId()).isEqualTo(7L);
        assertThat(post.getValue().title()).isEqualTo("제목");
        assertThat(post.getValue().content()).isEqualTo("내용");
        verify(repository).savePostFiles(99L, List.of(20L, 10L));
        assertThat(postId).isEqualTo(99L);
    }

    @Test
    void rejectsMissingMemberBeforeSavingPost() {
        var repository = mock(CommunityRepository.class);
        var members = mock(CommunityMemberReader.class);
        var files = mock(CommunityFileReader.class);
        when(members.exists(7L)).thenReturn(false);
        var service = new CommunityPostCommandService(repository, members, files);

        assertThatThrownBy(() -> service.write(7L, "제목", "내용", List.of()))
            .isInstanceOf(CustomException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsMissingFileBeforeSavingPost() {
        var repository = mock(CommunityRepository.class);
        var members = mock(CommunityMemberReader.class);
        var files = mock(CommunityFileReader.class);
        when(members.exists(7L)).thenReturn(true);
        when(files.findExistingIds(List.of(20L, 10L))).thenReturn(Set.of(20L));
        var service = new CommunityPostCommandService(repository, members, files);

        assertThatThrownBy(() -> service.write(7L, "제목", "내용", List.of(20L, 10L)))
            .isInstanceOf(CustomException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsNullFileIdBeforeLookingUpFiles() {
        var repository = mock(CommunityRepository.class);
        var members = mock(CommunityMemberReader.class);
        var files = mock(CommunityFileReader.class);
        when(members.exists(7L)).thenReturn(true);
        var service = new CommunityPostCommandService(repository, members, files);

        assertThatThrownBy(() -> service.write(7L, "제목", "내용", java.util.Arrays.asList(20L, null)))
            .isInstanceOf(CustomException.class);
        verify(files, never()).findExistingIds(any());
        verify(repository, never()).save(any());
    }

    @Test
    void updatesOwnedPostAndReplacesFiles() {
        var repository = mock(CommunityRepository.class);
        var members = mock(CommunityMemberReader.class);
        var files = mock(CommunityFileReader.class);
        when(repository.findActivePost(99L)).thenReturn(Optional.of(post(7L)));
        when(files.findExistingIds(List.of(20L, 10L))).thenReturn(Set.of(20L, 10L));
        var service = new CommunityPostCommandService(repository, members, files);

        service.update(7L, 99L, " 새 제목 ", " 새 내용 ", List.of(20L, 10L));

        var updated = ArgumentCaptor.forClass(CommunityPost.class);
        verify(repository).save(updated.capture());
        assertThat(updated.getValue().title()).isEqualTo("새 제목");
        assertThat(updated.getValue().content()).isEqualTo("새 내용");
        verify(repository).replacePostFiles(99L, List.of(20L, 10L));
    }

    @Test
    void rejectsUpdatingAnotherMembersPost() {
        var repository = mock(CommunityRepository.class);
        var members = mock(CommunityMemberReader.class);
        var files = mock(CommunityFileReader.class);
        when(repository.findActivePost(99L)).thenReturn(Optional.of(post(8L)));
        var service = new CommunityPostCommandService(repository, members, files);

        assertThatThrownBy(() -> service.update(7L, 99L, "제목", "내용", List.of()))
            .isInstanceOf(CustomException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void softDeletesOwnedPost() {
        var repository = mock(CommunityRepository.class);
        var members = mock(CommunityMemberReader.class);
        var files = mock(CommunityFileReader.class);
        when(repository.findActivePost(99L)).thenReturn(Optional.of(post(7L)));
        var service = new CommunityPostCommandService(repository, members, files);

        service.delete(7L, 99L);

        var deleted = ArgumentCaptor.forClass(CommunityPost.class);
        verify(repository).save(deleted.capture());
        assertThat(deleted.getValue().deletedAt()).isNotNull();
    }

    private CommunityPost post(Long memberId) {
        return new CommunityPost(99L, memberId, "제목", "내용", LocalDateTime.of(2026, 9, 3, 12, 0), null);
    }

}
