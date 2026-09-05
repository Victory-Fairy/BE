package kr.co.victoryfairy.game.domain;

import java.time.LocalDateTime;

public record Team(Long id, String name, String kboName, String sponsorName, String label, Short order,
        MatchEnum.LeagueType league, String countryCode, Boolean active, LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKboNm() {
        return kboName;
    }

    public String getSponsorNm() {
        return sponsorName;
    }

    public String getLabel() {
        return label;
    }

    public Short getOrderNo() {
        return order;
    }

    public MatchEnum.LeagueType getLeague() {
        return league;
    }

    public String getCountryCode() {
        return countryCode;
    }
}
