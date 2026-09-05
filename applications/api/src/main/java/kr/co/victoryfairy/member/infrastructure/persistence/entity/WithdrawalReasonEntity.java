package kr.co.victoryfairy.member.infrastructure.persistence.entity;

import kr.co.victoryfairy.diary.infrastructure.persistence.entity.*;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.*;
import kr.co.victoryfairy.shared.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "withdrawal_reason")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalReasonEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reason")
    private String reason;

}
