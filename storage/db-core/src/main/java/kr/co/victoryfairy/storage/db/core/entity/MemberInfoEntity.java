package kr.co.victoryfairy.storage.db.core.entity;

import io.dodn.springboot.core.enums.MemberEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;

@Entity(name = "member_info")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@DynamicInsert
@DynamicUpdate
public class MemberInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("회원 정보 ID")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberEntity memberEntity;

    @Comment("응원 팀")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private TeamEntity teamEntity;

    @Comment("sns 아이디")
    @Column(nullable = false)
    private String snsId;

    @Comment("이메일")
    @Column(length = 40, nullable = false)
    private String email;

    @Comment("닉네임")
    private String nickNm;

    @Comment("소셜 타입")
    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, columnDefinition = "enum('KAKAO', 'NAVER', 'GOOGLE', 'APPLE')")
    private MemberEnum.SnsType snsType;

    @Comment("생성일시")
    @CreationTimestamp
    @Column(columnDefinition = "TIMESTAMP", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Comment("수정일시")
    @UpdateTimestamp
    @Column(columnDefinition = "TIMESTAMP", insertable = false)
    private LocalDateTime updatedAt;

}
