package kr.co.victoryfairy.community.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostJpaRepository extends JpaRepository<CommunityPostJpaEntity, Long> {

    @Query("""
        SELECT p
        FROM community_post p
        WHERE p.deletedAt IS NULL
          AND (:cursor IS NULL OR p.id < :cursor)
          AND (:keyword IS NULL
               OR p.title LIKE CONCAT('%', :keyword, '%')
               OR p.content LIKE CONCAT('%', :keyword, '%'))
        ORDER BY p.id DESC
        """)
    List<CommunityPostJpaEntity> findPosts(@Param("cursor") Long cursor, @Param("keyword") String keyword,
            Pageable pageable);

    Optional<CommunityPostJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    @Query(value = """
        SELECT post_id, COUNT(*)
        FROM community_post_like
        WHERE post_id IN (:postIds)
        GROUP BY post_id
        """, nativeQuery = true)
    List<Object[]> countLikes(@Param("postIds") List<Long> postIds);

    @Query(value = """
        SELECT post_id, COUNT(*)
        FROM community_comment
        WHERE post_id IN (:postIds)
          AND deleted_at IS NULL
        GROUP BY post_id
        """, nativeQuery = true)
    List<Object[]> countComments(@Param("postIds") List<Long> postIds);

    @Query(value = """
        SELECT post_id
        FROM community_post_like
        WHERE member_id = :memberId
          AND post_id IN (:postIds)
        """, nativeQuery = true)
    List<Long> findLikedPostIds(@Param("memberId") Long memberId, @Param("postIds") List<Long> postIds);

    @Modifying
    @Query(value = """
        INSERT IGNORE INTO community_post_like (post_id, member_id)
        VALUES (:postId, :memberId)
        """, nativeQuery = true)
    void addLike(@Param("postId") Long postId, @Param("memberId") Long memberId);

    @Modifying
    @Query(value = """
        DELETE FROM community_post_like
        WHERE post_id = :postId AND member_id = :memberId
        """, nativeQuery = true)
    void deleteLike(@Param("postId") Long postId, @Param("memberId") Long memberId);

}
