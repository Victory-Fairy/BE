package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "seat")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 좌석 식별자

    @ManyToOne
    @JoinColumn(name = "stadium_id")
    private StadiumEntity stadiumEntity; // 경기장 식별자

    private String name; // 이름

    private String season;

}
