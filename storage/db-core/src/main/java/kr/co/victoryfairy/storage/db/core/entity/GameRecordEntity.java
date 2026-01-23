package kr.co.victoryfairy.storage.db.core.entity;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.MatchEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity(name = "game_record")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameRecordEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private MemberEntity member; // 회원 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id")
    private DiaryEntity diaryEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_match_id")
    private GameMatchEntity gameMatchEntity; // 경기 식별자

    @Comment("응원 팀 id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private TeamEntity teamEntity;

    @Column
    private String teamName;

    @Comment("상대 팀 id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opponent_team_id")
    private TeamEntity opponentTeamEntity;

    @Column
    private String opponentTeamName;

    @Comment("경기장")
    @JoinColumn(name = "stadium_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private StadiumEntity stadiumEntity;

    @Column
    @Comment("관람 타입")
    @Enumerated(EnumType.STRING)
    private DiaryEnum.ViewType viewType;

    @Column
    @Comment("경기 상태")
    @Enumerated(EnumType.STRING)
    private MatchEnum.MatchStatus status;

    @Column
    @Comment("경기 결과")
    @Enumerated(EnumType.STRING)
    private MatchEnum.ResultType resultType;

    private String season;

    @Column(length = 10)
    @Comment("리그 타입 (KBO, WBC, MLB)")
    @Enumerated(EnumType.STRING)
    private MatchEnum.LeagueType leagueType;

    public void updateRecord(TeamEntity teamEntity, TeamEntity opponentTeamEntity, MatchEnum.ResultType resultType) {
        this.teamEntity = teamEntity;
        this.teamName = teamEntity.getName();
        this.opponentTeamEntity = opponentTeamEntity;
        this.opponentTeamName = opponentTeamEntity.getName();
        this.resultType = resultType;
    }

}
