package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "seat_use_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatUseHistoryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 좌석 이용 내역 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    private SeatEntity seatEntity; // 좌석 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id")
    private DiaryEntity diaryEntity; // 일기 식별자

    @Column(name = "seat_name")
    private String seatName; // 좌석 번호

}
