package kr.co.victoryfairy.storage.db.core.entity;

import io.dodn.springboot.core.enums.RefType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity(name = "diary_food")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryFoodEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 일기 음식 식별자

    @Comment("참조 ID")
    @Column(name = "ref_id")
    private Long refId;

    @Comment("참조 구분")
    @Enumerated(EnumType.STRING)
    private RefType refType;

    @Column(name = "food_name")
    private String foodName; // 음식 이름

}
