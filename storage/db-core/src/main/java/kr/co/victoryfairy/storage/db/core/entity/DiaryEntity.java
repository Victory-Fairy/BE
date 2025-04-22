package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "diary")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryEntity extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                        // 일기 식별자

    @ManyToOne
    @JoinColumn(name = "member_id")
    private MemberEntity member;            // 회원 식별자

    @ManyToOne
    @JoinColumn(name = "game_match_id")
    private GameMatchEntity gameMatch;      // 경기 식별자

    @ManyToOne
    @JoinColumn(name = "team_id")
    private TeamEntity team;                // 응원팀 식별자

    @Column(name = "team_name")
    private String teamName;                // 응원팀

    @Column(name = "view_type")
    private String viewType;                // 관람 방식

    private String weather;                 // 날씨

    private String memo;                    // 메모

}
