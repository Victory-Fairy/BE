package kr.co.victoryfairy.community.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import kr.co.victoryfairy.community.domain.CommunityComment;
import kr.co.victoryfairy.community.domain.CommunityPost;
import kr.co.victoryfairy.community.domain.CommunityPostFile;
import kr.co.victoryfairy.community.domain.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaCommunityRepository implements CommunityRepository {

    private final CommunityPostJpaRepository posts;

    private final CommunityPostFileJpaRepository postFiles;

    private final CommunityCommentJpaRepository comments;

    @Override
    public CommunityPost save(CommunityPost post) {
        return posts.save(CommunityPostJpaEntity.from(post)).toDomain();
    }

    @Override
    public void savePostFiles(Long postId, List<Long> fileIds) {
        postFiles.saveAll(fileIds.stream()
            .map(fileId -> new CommunityPostFileJpaEntity(null, postId, fileId))
            .toList());
    }

    @Override
    public List<CommunityPost> findPosts(Long cursor, String keyword, int limit) {
        return posts.findPosts(cursor, keyword, PageRequest.of(0, limit)).stream()
            .map(CommunityPostJpaEntity::toDomain)
            .toList();
    }

    @Override
    public Optional<CommunityPost> findActivePost(Long postId) {
        return posts.findByIdAndDeletedAtIsNull(postId).map(CommunityPostJpaEntity::toDomain);
    }

    @Override
    public List<CommunityPostFile> findPostFiles(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        return postFiles.findByPostIdInOrderByIdAsc(postIds).stream()
            .map(CommunityPostFileJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<CommunityComment> findComments(Long postId, Long cursor, int limit) {
        return comments.findComments(postId, cursor, PageRequest.of(0, limit)).stream()
            .map(CommunityCommentJpaEntity::toDomain)
            .toList();
    }

    @Override
    public Map<Long, Long> countPostLikes(List<Long> postIds) {
        return postIds.isEmpty() ? Map.of() : counts(posts.countLikes(postIds));
    }

    @Override
    public Map<Long, Long> countPostComments(List<Long> postIds) {
        return postIds.isEmpty() ? Map.of() : counts(posts.countComments(postIds));
    }

    @Override
    public Set<Long> findLikedPostIds(Long memberId, List<Long> postIds) {
        return postIds.isEmpty() ? Set.of() : Set.copyOf(posts.findLikedPostIds(memberId, postIds));
    }

    @Override
    public Map<Long, Long> countCommentLikes(List<Long> commentIds) {
        return commentIds.isEmpty() ? Map.of() : counts(comments.countLikes(commentIds));
    }

    @Override
    public Set<Long> findLikedCommentIds(Long memberId, List<Long> commentIds) {
        return commentIds.isEmpty() ? Set.of() : Set.copyOf(comments.findLikedCommentIds(memberId, commentIds));
    }

    private Map<Long, Long> counts(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
            row -> ((Number) row[0]).longValue(),
            row -> ((Number) row[1]).longValue()));
    }

}
