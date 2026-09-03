package kr.co.victoryfairy.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

import kr.co.victoryfairy.community.domain.CommunityComment;
import kr.co.victoryfairy.community.domain.CommunityPost;
import kr.co.victoryfairy.community.domain.CommunityPostFile;
import kr.co.victoryfairy.community.domain.CommunityRepository;
import org.junit.jupiter.api.Test;

class CommunityQueryServiceTest {

    @Test
    void returnsTwentyPostsAndUsesLastReturnedIdAsNextCursor() {
        var repository = mock(CommunityRepository.class);
        var members = mock(CommunityMemberReader.class);
        var files = mock(CommunityFileReader.class);
        var posts = LongStream.iterate(100L, id -> id - 1)
            .limit(21)
            .mapToObj(id -> post(id, 7L))
            .toList();
        when(repository.findPosts(null, null, 21)).thenReturn(posts);
        when(members.findAuthors(List.of(7L)))
            .thenReturn(Map.of(7L, new CommunityMemberReader.Author(7L, "작성자", null)));
        var service = new CommunityQueryService(repository, members, files);

        var result = service.findPosts(7L, null, "  ");

        assertThat(result.items()).hasSize(20);
        assertThat(result.nextCursor()).isEqualTo(81L);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.items().getFirst().author().nickname()).isEqualTo("작성자");
    }

    @Test
    void rendersDeletedCommentWithoutAuthorOrLikeData() {
        var repository = mock(CommunityRepository.class);
        var members = mock(CommunityMemberReader.class);
        var files = mock(CommunityFileReader.class);
        var deletedAt = LocalDateTime.of(2026, 9, 3, 12, 0);
        when(repository.findActivePost(99L)).thenReturn(java.util.Optional.of(post(99L, 7L)));
        when(repository.findComments(99L, null, 21)).thenReturn(List.of(
            new CommunityComment(31L, 99L, 8L, "원래 내용", deletedAt.minusMinutes(1), deletedAt)));
        var service = new CommunityQueryService(repository, members, files);

        var result = service.findComments(7L, 99L, null);

        assertThat(result.items()).singleElement().satisfies(comment -> {
            assertThat(comment.content()).isEqualTo("삭제된 댓글입니다.");
            assertThat(comment.deleted()).isTrue();
            assertThat(comment.author()).isNull();
            assertThat(comment.likeCount()).isZero();
        });
    }

    @Test
    void composesPostDetailFromBatchedIds() {
        var repository = mock(CommunityRepository.class);
        var members = mock(CommunityMemberReader.class);
        var files = mock(CommunityFileReader.class);
        when(repository.findActivePost(99L)).thenReturn(java.util.Optional.of(post(99L, 7L)));
        when(repository.findPostFiles(List.of(99L))).thenReturn(List.of(
            new CommunityPostFile(1L, 99L, 20L),
            new CommunityPostFile(2L, 99L, 10L)));
        when(repository.countPostLikes(List.of(99L))).thenReturn(Map.of(99L, 3L));
        when(repository.countPostComments(List.of(99L))).thenReturn(Map.of(99L, 2L));
        when(repository.findLikedPostIds(7L, List.of(99L))).thenReturn(Set.of(99L));
        when(repository.findComments(99L, null, 21)).thenReturn(List.of());
        when(members.findAuthors(List.of(7L)))
            .thenReturn(Map.of(7L, new CommunityMemberReader.Author(7L, "작성자", "profile-url")));
        when(files.findUrls(List.of(20L, 10L))).thenReturn(Map.of(20L, "url-20", 10L, "url-10"));
        var service = new CommunityQueryService(repository, members, files);

        var result = service.findPost(7L, 99L);

        assertThat(result.likeCount()).isEqualTo(3);
        assertThat(result.commentCount()).isEqualTo(2);
        assertThat(result.likedByMe()).isTrue();
        assertThat(result.mine()).isTrue();
        assertThat(result.author().profileImageUrl()).isEqualTo("profile-url");
        assertThat(result.images()).extracting(CommunityView.Image::url)
            .containsExactly("url-20", "url-10");
    }

    private CommunityPost post(Long id, Long memberId) {
        return new CommunityPost(id, memberId, "제목 " + id, "내용 " + id,
                LocalDateTime.of(2026, 9, 3, 12, 0), null);
    }

}
