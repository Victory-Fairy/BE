package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity(name = "admin")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String adminId;

    @Column
    private String pwd;

    @Comment("마지막 접속 아이피")
    private String lastConnectIp;

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

}
