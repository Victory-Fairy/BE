package kr.co.victoryfairy.diary.infrastructure.persistence.entity;

import kr.co.victoryfairy.game.infrastructure.persistence.entity.*;
import kr.co.victoryfairy.shared.infrastructure.persistence.entity.BaseEntity;

import kr.co.victoryfairy.diary.domain.DiaryEnum;
import kr.co.victoryfairy.diary.domain.Diary;
import kr.co.victoryfairy.game.domain.MatchEnum;
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

    @Column(name = "member_id")
    private Long memberId; // 회원 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_match_id")
    private GameMatchEntity gameMatchEntity; // 경기 식별자

    @Column(name = "team_name")
    private String teamName; // 응원팀

    @Comment("응원 팀 id")
    @ManyToOne(fetch = FetchType.LAZY)
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

    public void apply(Diary diary, GameMatchEntity match, TeamEntity team) {
        this.memberId = diary.memberId();
        this.gameMatchEntity = match;
        this.teamEntity = team;
        this.teamName = diary.teamName();
        this.viewType = diary.viewType();
        this.weatherType = diary.weather();
        this.moodType = diary.mood();
        this.content = diary.content();
        this.isRated = diary.rated();
    }

}
