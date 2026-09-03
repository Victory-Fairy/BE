package kr.co.victoryfairy.community.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface CommunityRepository {

    CommunityPost save(CommunityPost post);

    boolean updatePost(CommunityPost post);

    boolean deletePost(CommunityPost post);

    void savePostFiles(Long postId, List<Long> fileIds);

    void replacePostFiles(Long postId, List<Long> fileIds);

    CommunityComment saveComment(CommunityComment comment);

    boolean updateComment(CommunityComment comment);

    boolean deleteComment(CommunityComment comment);

    List<CommunityPost> findPosts(Long cursor, String keyword, int limit);

    Optional<CommunityPost> findActivePost(Long postId);

    Optional<CommunityComment> findActiveComment(Long postId, Long commentId);

    void setPostLike(Long postId, Long memberId, boolean liked);

    void setCommentLike(Long commentId, Long memberId, boolean liked);

    List<CommunityPostFile> findPostFiles(List<Long> postIds);

    List<CommunityComment> findComments(Long postId, Long cursor, int limit);

    Map<Long, Long> countPostLikes(List<Long> postIds);

    Map<Long, Long> countPostComments(List<Long> postIds);

    Set<Long> findLikedPostIds(Long memberId, List<Long> postIds);

    Map<Long, Long> countCommentLikes(List<Long> commentIds);

    Set<Long> findLikedCommentIds(Long memberId, List<Long> commentIds);

}
