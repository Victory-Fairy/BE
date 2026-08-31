package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.Comment;

@Entity(name = "winning_rate")
@Getter
@AllArgsConstructor
@Builder
public class WinningRateEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private MemberEntity member; // 회원 식별자

    @Comment("시즌")
    private String season;

    @Comment("시즌 총 기록 수")
    private Short totalCnt;

    @Comment("전체 승 수")
    private Short totalWinCnt;

    @Comment("전체 승률")
    private Float totalAvg;

    @Comment("집관 기룩 수")
    private Short homeCnt;

    @Comment("집관 승 수")
    private Short homeWinCnt;

    @Comment("집관 승률")
    private Float homeAvg;

    @Comment("직관 기록 수")
    private Short stadiumCnt;

    @Comment("직관 승 수")
    private Short stadiumWinCnt;

    @Comment("직관 승률")
    private Float stadiumAvg;

    public WinningRateEntity() {
    }

    public WinningRateEntity(MemberEntity member, String season) {
        this.member = member;
        this.season = season;
    }

    public void updateWinningRate(Short totalCnt, Short totalWinCnt, Float totalAvg, Short homeCnt, Short homeWinCnt,
            Float homeAvg, Short stadiumCnt, Short stadiumWinCnt, Float stadiumAvg) {
        this.totalCnt = totalCnt;
        this.totalWinCnt = totalWinCnt;
        this.totalAvg = totalAvg;
        this.homeCnt = homeCnt;
        this.homeWinCnt = homeWinCnt;
        this.homeAvg = homeAvg;
        this.stadiumCnt = stadiumCnt;
        this.stadiumWinCnt = stadiumWinCnt;
        this.stadiumAvg = stadiumAvg;
    }

}
