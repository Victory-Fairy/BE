package kr.co.victoryfairy.storage.db.core.entity;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.MatchEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity(name = "diary")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 일기 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberEntity member; // 회원 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_match_id")
    private GameMatchEntity gameMatchEntity; // 경기 식별자

    @Column(name = "team_name")
    private String teamName; // 응원팀

    @Comment("응원 팀 id")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private TeamEntity teamEntity;

    @Column(name = "view_type")
    @Enumerated(EnumType.STRING)
    private DiaryEnum.ViewType viewType; // 관람 방식

    @Column(name = "weather")
    @Enumerated(EnumType.STRING)
    private DiaryEnum.WeatherType weatherType; // 날씨

    @Column(name = "mood")
    @Enumerated(EnumType.STRING)
    private DiaryEnum.MoodType moodType;

    @Column(name = "content")
    private String content; // 메모

    @Column(columnDefinition = "bit(1) DEFAULT b'0'")
    @Builder.Default
    private Boolean isRated = false;

    public void updateRated() {
        this.isRated = true;
    }

    public void updateDiary(String teamName, TeamEntity teamEntity, DiaryEnum.ViewType viewType,
            DiaryEnum.MoodType moodType, DiaryEnum.WeatherType weather, String content) {
        this.teamName = teamName;
        this.teamEntity = teamEntity;
        this.viewType = viewType;
        this.moodType = moodType;
        this.weatherType = weather;
        this.content = content;
        update();
    }

}
