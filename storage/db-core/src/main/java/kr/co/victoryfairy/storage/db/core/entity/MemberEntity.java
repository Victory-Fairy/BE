package kr.co.victoryfairy.storage.db.core.entity;

import io.dodn.springboot.core.enums.MemberEnum;
import jakarta.persistence.*;
import io.dodn.springboot.core.enums.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity(name = "member")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 회원 식별자

    @Comment("회원 상태")
    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, columnDefinition = "enum('NORMAL', 'WITHDRAWAL', 'CUTOFF') DEFAULT 'NORMAL'")
    private MemberEnum.Status status;

    @Comment("마지막 접속 아이피")
    private String lastConnectIp;

    @Comment("fcm 토큰")
    private String fcmToken;

    @Comment("사용여부")
    @Column(nullable = false, columnDefinition = "bit(1) DEFAULT b'1'")
    @Builder.Default
    private Boolean isUse = true;

    @Comment("생성일시")
    @CreationTimestamp
    @Column(columnDefinition = "TIMESTAMP", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Comment("수정일시")
    @UpdateTimestamp
    @Column(columnDefinition = "TIMESTAMP", insertable = false)
    private LocalDateTime updatedAt;

    @Comment("마지막 접속 일시")
    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime lastConnectAt;

    public void updateLastLogin(String lastConnectIp, LocalDateTime lastConnectAt) {
        this.lastConnectIp = lastConnectIp;
        this.lastConnectAt = lastConnectAt;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

}
