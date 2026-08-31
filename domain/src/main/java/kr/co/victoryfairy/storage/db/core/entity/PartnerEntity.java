package kr.co.victoryfairy.storage.db.core.entity;

import io.dodn.springboot.core.enums.RefType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity(name = "partner")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 함께한 사람 식별자

    @Comment("참조 ID")
    @Column(name = "ref_id")
    private Long refId;

    @Comment("참조 구분")
    @Enumerated(EnumType.STRING)
    private RefType refType;

    private String name; // 함께한 사람 이름

    @Column(name = "team_name")
    private String teamName; // 함께한 사람의 응원팀

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private TeamEntity teamEntity;

}
