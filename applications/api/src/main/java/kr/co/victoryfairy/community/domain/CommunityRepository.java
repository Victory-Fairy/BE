package kr.co.victoryfairy.community.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface CommunityRepository {

    CommunityPost save(CommunityPost post);

    void savePostFiles(Long postId, List<Long> fileIds);

    List<CommunityPost> findPosts(Long cursor, String keyword, int limit);

    Optional<CommunityPost> findActivePost(Long postId);

    List<CommunityPostFile> findPostFiles(List<Long> postIds);

    List<CommunityComment> findComments(Long postId, Long cursor, int limit);

    Map<Long, Long> countPostLikes(List<Long> postIds);

    Map<Long, Long> countPostComments(List<Long> postIds);

    Set<Long> findLikedPostIds(Long memberId, List<Long> postIds);

    Map<Long, Long> countCommentLikes(List<Long> commentIds);

    Set<Long> findLikedCommentIds(Long memberId, List<Long> commentIds);

}
