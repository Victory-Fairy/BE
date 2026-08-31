package kr.co.victoryfairy.storage.db.core.entity;

import jakarta.persistence.*;
import io.dodn.springboot.core.enums.MatchEnum;
import org.hibernate.annotations.Comment;

@Entity(name = "team")
public class TeamEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @Comment("팀명")
    private String name;

    @Column
    @Comment("kbo 팀명")
    private String kboNm;

    @Column
    @Comment("스폰서 명")
    private String sponsorNm;

    @Column
    private String label;

    @Column
    private Short orderNo;

    @Column(length = 10)
    @Enumerated(EnumType.STRING)
    @Comment("리그 타입 (KBO, WBC, MLB)")
    private MatchEnum.LeagueType league = MatchEnum.LeagueType.KBO;

    @Column(length = 10)
    @Comment("WBC 국가 코드 (KOR, JPN, USA 등)")
    private String countryCode;

    public TeamEntity() {
    }

    public TeamEntity(Long id, String name, String kboNm) {
        this.id = id;
        this.name = name;
        this.kboNm = kboNm;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKboNm() {
        return kboNm;
    }

    public String getSponsorNm() {
        return sponsorNm;
    }

    public String getLabel() {
        return label;
    }

    public Short getOrderNo() {
        return orderNo;
    }

    public MatchEnum.LeagueType getLeague() {
        return league;
    }

    public String getCountryCode() {
        return countryCode;
    }

}
