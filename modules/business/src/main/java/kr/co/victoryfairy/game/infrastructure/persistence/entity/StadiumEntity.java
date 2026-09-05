package kr.co.victoryfairy.game.infrastructure.persistence.entity;

import kr.co.victoryfairy.diary.infrastructure.persistence.entity.*;
import kr.co.victoryfairy.shared.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "stadium")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StadiumEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    private String shortName;

    private String region;

    private Integer externalId;

}
