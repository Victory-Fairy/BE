package kr.co.victoryfairy.community.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import kr.co.victoryfairy.community.domain.CommunityPost;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JpaCommunityRepositoryTest {

    @Test
    void savesPostUsingMemberIdWithoutMemberEntity() {
        var posts = mock(CommunityPostJpaRepository.class);
        var files = mock(CommunityPostFileJpaRepository.class);
        var comments = mock(CommunityCommentJpaRepository.class);
        var repository = new JpaCommunityRepository(posts, files, comments);
        when(posts.save(any())).thenAnswer(invocation -> {
            var entity = invocation.getArgument(0, CommunityPostJpaEntity.class);
            return new CommunityPostJpaEntity(99L, entity.getMemberId(), entity.getTitle(), entity.getContent(),
                    null, null);
        });

        var saved = repository.save(CommunityPost.create(7L, "제목", "내용"));

        var captured = ArgumentCaptor.forClass(CommunityPostJpaEntity.class);
        verify(posts).save(captured.capture());
        assertThat(captured.getValue().getMemberId()).isEqualTo(7L);
        assertThat(saved.id()).isEqualTo(99L);
        assertThat(saved.memberId()).isEqualTo(7L);
    }

    @Test
    void keepsRequestedFileOrderWhenSavingPostFiles() {
        var posts = mock(CommunityPostJpaRepository.class);
        var files = mock(CommunityPostFileJpaRepository.class);
        var comments = mock(CommunityCommentJpaRepository.class);
        var repository = new JpaCommunityRepository(posts, files, comments);

        repository.savePostFiles(99L, List.of(20L, 10L));

        @SuppressWarnings({ "rawtypes", "unchecked" })
        ArgumentCaptor<List<CommunityPostFileJpaEntity>> captured = (ArgumentCaptor) ArgumentCaptor
            .forClass(List.class);
        verify(files).saveAll(captured.capture());
        assertThat(captured.getValue()).extracting(CommunityPostFileJpaEntity::getFileId)
            .containsExactly(20L, 10L);
    }

}
