package kr.co.victoryfairy.storage.db.core.entity;

import io.dodn.springboot.core.enums.MatchEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity(name = "game_match")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@DynamicInsert
@DynamicUpdate
public class GameMatchEntity extends BaseEntity {

    @Id
    private String id;

    @Column
    @Comment("경기 타입")
    @Enumerated(EnumType.STRING)
    private MatchEnum.MatchType type;

    @Column
    @Comment("시리즈 타입")
    @Enumerated(EnumType.STRING)
    private MatchEnum.SeriesType series;

    @Comment("시즌")
    private String season;

    @Column
    @Comment("경기 일자")
    private LocalDateTime matchAt;

    @Comment("어웨이")
    @JoinColumn(name = "away_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private TeamEntity awayTeamEntity;

    @Column
    @Comment("어웨이 팀명")
    private String awayNm;

    @Column
    @Comment("어웨이 점수")
    private Short awayScore;

    @Comment("홈")
    @JoinColumn(name = "home_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private TeamEntity homeTeamEntity;

    @Column
    @Comment("홈 팀명")
    private String homeNm;

    @Column
    @Comment("홈 스코어")
    private Short homeScore;

    @Comment("경기장")
    @JoinColumn(name = "stadium_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private StadiumEntity stadiumEntity;

    @Column
    @Comment("경기 상태")
    @Enumerated(EnumType.STRING)
    private MatchEnum.MatchStatus status;

    @Column
    @Comment("사유")
    private String reason;

    @Comment("경기 내용 크롤링 여부")
    @Column(columnDefinition = "bit(1) DEFAULT b'0'")
    @Builder.Default
    private Boolean isMatchInfoCraw = false;

    @Comment("경기 시작 알림 발송 여부")
    @Column(columnDefinition = "bit(1) DEFAULT b'0'")
    private Boolean isSendPush = false;
}
