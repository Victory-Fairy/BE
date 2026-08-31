package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Comment;

@Entity(name = "pitcher_record")
public class PitcherRecordEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @Comment("순서")
    private Short turn;

    @Column
    @Comment("선수명")
    private String name;

    @Column
    @Comment("포지션")
    private String position;

    @Column
    @Comment("이닝")
    private String inning;

    @Column
    @Comment("투구수")
    private Short pitching;

    @Column
    @Comment("4사구")
    private Short ballFour;

    @Column
    @Comment("삼진")
    private Short strikeOut;

    @Column
    @Comment("피안타")
    private Short hit;

    @Column
    @Comment("피홈런")
    private Short homeRun;

    @Column
    @Comment("실점")
    private Short score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_match_id")
    private GameMatchEntity gameMatchEntity;

    @Column
    @Comment("시즌")
    private String season;

    @Column
    @Comment("홈/어웨이 여부")
    private Boolean isHome;

    public PitcherRecordEntity() {
    }

    public PitcherRecordEntity(Short turn, String name, String position, String inning, Short pitching, Short ballFour,
            Short strikeOut, Short hit, Short homeRun, Short score, GameMatchEntity gameMatchEntities, String season,
            Boolean isHome) {
        this.turn = turn;
        this.name = name;
        this.position = position;
        this.inning = inning;
        this.pitching = pitching;
        this.ballFour = ballFour;
        this.strikeOut = strikeOut;
        this.hit = hit;
        this.homeRun = homeRun;
        this.score = score;
        this.gameMatchEntity = gameMatchEntities;
        this.season = season;
        this.isHome = isHome;
    }

    public Long getId() {
        return id;
    }

    public Short getTurn() {
        return turn;
    }

    public String getName() {
        return name;
    }

    public String getPosition() {
        return position;
    }

    public String getInning() {
        return inning;
    }

    public Short getPitching() {
        return pitching;
    }

    public Short getBallFour() {
        return ballFour;
    }

    public Short getStrikeOut() {
        return strikeOut;
    }

    public Short getHit() {
        return hit;
    }

    public Short getHomeRun() {
        return homeRun;
    }

    public Short getScore() {
        return score;
    }

    public String getSeason() {
        return season;
    }

    public GameMatchEntity getGameMatchEntity() {
        return gameMatchEntity;
    }

    public Boolean getHome() {
        return isHome;
    }

}
