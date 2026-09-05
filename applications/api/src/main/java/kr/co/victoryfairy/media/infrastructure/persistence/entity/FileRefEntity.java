package kr.co.victoryfairy.media.infrastructure.persistence.entity;

import kr.co.victoryfairy.diary.infrastructure.persistence.entity.*;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.*;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.*;
import kr.co.victoryfairy.shared.infrastructure.persistence.entity.BaseEntity;

import kr.co.victoryfairy.shared.domain.RefType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@DynamicUpdate
@Entity(name = "file_ref")
public class FileRefEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id")
    private FileEntity fileEntity;

    @Comment("참조 ID")
    @Column(name = "ref_id")
    private Long refId;

    @Comment("참조 구분")
    @Enumerated(EnumType.STRING)
    private RefType refType;

}
