package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Entity(name = "file")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@DynamicInsert
@DynamicUpdate
public class FileEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("파일 오리진 이름")
    @Column(length = 50)
    private String name;

    @Comment("파일 저장 이름")
    @Column(length = 50)
    private String saveName;

    @Comment("파일 경로")
    @Column(length = 50)
    private String path;

    @Comment("확장자")
    @Column(length = 20)
    private String ext;

    @Comment("Size")
    private Long size;

}
