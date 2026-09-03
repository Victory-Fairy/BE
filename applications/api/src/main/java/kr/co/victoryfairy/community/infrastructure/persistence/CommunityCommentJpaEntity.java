package kr.co.victoryfairy.community.infrastructure.persistence;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.co.victoryfairy.community.domain.CommunityComment;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "community_comment")
@Table(name = "community_comment")
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityCommentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 100)
    private String content;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    static CommunityCommentJpaEntity from(CommunityComment comment) {
        return new CommunityCommentJpaEntity(comment.id(), comment.postId(), comment.memberId(), comment.content(),
                comment.createdAt(), comment.deletedAt());
    }

    CommunityComment toDomain() {
        return new CommunityComment(id, postId, memberId, content, createdAt, deletedAt);
    }

}
