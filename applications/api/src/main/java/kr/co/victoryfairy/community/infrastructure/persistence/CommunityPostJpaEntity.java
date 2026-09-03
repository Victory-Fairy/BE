package kr.co.victoryfairy.community.infrastructure.persistence;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.co.victoryfairy.community.domain.CommunityPost;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "community_post")
@Table(name = "community_post")
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 30)
    private String title;

    @Column(nullable = false, length = 100)
    private String content;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    static CommunityPostJpaEntity from(CommunityPost post) {
        return new CommunityPostJpaEntity(post.id(), post.memberId(), post.title(), post.content(),
                post.createdAt(), post.deletedAt());
    }

    CommunityPost toDomain() {
        return new CommunityPost(id, memberId, title, content, createdAt, deletedAt);
    }

}
