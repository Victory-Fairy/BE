package kr.co.victoryfairy.diary.infrastructure.persistence.entity;

import kr.co.victoryfairy.game.infrastructure.persistence.entity.*;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.*;
import kr.co.victoryfairy.shared.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "seat_review")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatReviewEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 좌석 리뷰 식별자

    @ManyToOne
    @JoinColumn(name = "seat_use_history_id")
    private SeatUseHistoryEntity seatUseHistoryEntity; // 좌석 이용 내역 식별자

    private String seatReview; // 좌석 리뷰

}
