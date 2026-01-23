package kr.co.victoryfairy.storage.db.core.entity;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.MatchEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity(name = "free_diary")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreeDiaryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberEntity member;

    // 경기 정보 (직접 입력)
    @Comment("경기 상태")
    @Column(name = "match_status")
    @Enumerated(EnumType.STRING)
    private MatchEnum.MatchStatus matchStatus;

    @Comment("홈팀 이름")
    @Column(name = "home_team_name")
    private String homeTeamName;

    @Comment("어웨이팀 이름")
    @Column(name = "away_team_name")
    private String awayTeamName;

    @Comment("홈팀 점수")
    @Column(name = "home_score")
    private Short homeScore;

    @Comment("어웨이팀 점수")
    @Column(name = "away_score")
    private Short awayScore;

    @Comment("경기장 이름")
    @Column(name = "stadium_name")
    private String stadiumName;

    @Comment("경기 일시")
    @Column(name = "match_at")
    private LocalDateTime matchAt;

    // 응원 정보
    @Comment("응원팀 이름")
    @Column(name = "team_name")
    private String teamName;

    @Comment("관람 방식")
    @Column(name = "view_type")
    @Enumerated(EnumType.STRING)
    private DiaryEnum.ViewType viewType;

    // 일기 내용
    @Comment("기분")
    @Column(name = "mood")
    @Enumerated(EnumType.STRING)
    private DiaryEnum.MoodType moodType;

    @Comment("날씨")
    @Column(name = "weather")
    @Enumerated(EnumType.STRING)
    private DiaryEnum.WeatherType weatherType;

    @Comment("내용")
    @Column(name = "content")
    private String content;

    @Comment("좌석 후기")
    @Column(name = "seat_review")
    private String seatReview;

    public void updateFreeDiary(MatchEnum.MatchStatus matchStatus, String homeTeamName, String awayTeamName,
            Short homeScore, Short awayScore, String stadiumName, LocalDateTime matchAt, String teamName,
            DiaryEnum.ViewType viewType, DiaryEnum.MoodType moodType, DiaryEnum.WeatherType weatherType, String content,
            String seatReview) {
        this.matchStatus = matchStatus;
        this.homeTeamName = homeTeamName;
        this.awayTeamName = awayTeamName;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.stadiumName = stadiumName;
        this.matchAt = matchAt;
        this.teamName = teamName;
        this.viewType = viewType;
        this.moodType = moodType;
        this.weatherType = weatherType;
        this.content = content;
        this.seatReview = seatReview;
        update();
    }

}