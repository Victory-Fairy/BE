package kr.co.victoryfairy.community.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityCommentJpaRepository extends JpaRepository<CommunityCommentJpaEntity, Long> {

    @Query("""
        SELECT c
        FROM community_comment c
        WHERE c.postId = :postId
          AND c.deletedAt IS NULL
          AND (:cursor IS NULL OR c.id > :cursor)
        ORDER BY c.id ASC
        """)
    List<CommunityCommentJpaEntity> findComments(@Param("postId") Long postId, @Param("cursor") Long cursor,
            Pageable pageable);

    Optional<CommunityCommentJpaEntity> findByIdAndPostIdAndDeletedAtIsNull(Long id, Long postId);

    @Query(value = """
        SELECT comment_id, COUNT(*)
        FROM community_comment_like
        WHERE comment_id IN (:commentIds)
        GROUP BY comment_id
        """, nativeQuery = true)
    List<Object[]> countLikes(@Param("commentIds") List<Long> commentIds);

    @Query(value = """
        SELECT comment_id
        FROM community_comment_like
        WHERE member_id = :memberId
          AND comment_id IN (:commentIds)
        """, nativeQuery = true)
    List<Long> findLikedCommentIds(@Param("memberId") Long memberId, @Param("commentIds") List<Long> commentIds);

    @Modifying
    @Query(value = """
        INSERT IGNORE INTO community_comment_like (comment_id, member_id)
        VALUES (:commentId, :memberId)
        """, nativeQuery = true)
    void addLike(@Param("commentId") Long commentId, @Param("memberId") Long memberId);

    @Modifying
    @Query(value = """
        DELETE FROM community_comment_like
        WHERE comment_id = :commentId AND member_id = :memberId
        """, nativeQuery = true)
    void deleteLike(@Param("commentId") Long commentId, @Param("memberId") Long memberId);

}
