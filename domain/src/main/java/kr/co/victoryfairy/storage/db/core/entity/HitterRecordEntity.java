package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Comment;

@Entity(name = "hitter_record")
public class HitterRecordEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @Comment("타순")
    private Short turn;

    @Column
    @Comment("선수명")
    private String name;

    @Column
    @Comment("포지션")
    private String position;

    @Column
    @Comment("타수")
    private Short hitCount;

    @Column
    @Comment("득점")
    private Short score;

    @Column
    @Comment("안타")
    private Short hit;

    @Column
    @Comment("홈런")
    private Short homeRun;

    @Column
    @Comment("타점")
    private Short hitScore;

    @Column
    @Comment("4사구")
    private Short ballFour;

    @Column
    @Comment("삼진")
    private Short strikeOut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_match_id")
    private GameMatchEntity gameMatchEntity;

    @Column
    @Comment("시즌")
    private String season;

    @Column
    @Comment("홈/어웨이 여부")
    private Boolean isHome;

    public HitterRecordEntity() {
    }

    public HitterRecordEntity(Short turn, String name, String position, Short hitCount, Short score, Short hit,
            Short homeRun, Short hitScore, Short ballFour, Short strikeOut, GameMatchEntity gameMatchEntity,
            String season, Boolean isHome) {
        this.turn = turn;
        this.name = name;
        this.position = position;
        this.hitCount = hitCount;
        this.score = score;
        this.hit = hit;
        this.homeRun = homeRun;
        this.hitScore = hitScore;
        this.ballFour = ballFour;
        this.strikeOut = strikeOut;
        this.gameMatchEntity = gameMatchEntity;
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

    public Short getHitCount() {
        return hitCount;
    }

    public Short getScore() {
        return score;
    }

    public Short getHit() {
        return hit;
    }

    public Short getHomeRun() {
        return homeRun;
    }

    public Short getHitScore() {
        return hitScore;
    }

    public Short getBallFour() {
        return ballFour;
    }

    public Short getStrikeOut() {
        return strikeOut;
    }

    public GameMatchEntity getGameMatchEntity() {
        return gameMatchEntity;
    }

    public String getSeason() {
        return season;
    }

    public Boolean getHome() {
        return isHome;
    }

}
