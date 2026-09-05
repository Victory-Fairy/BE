package kr.co.victoryfairy.game.domain;

import java.time.LocalDateTime;

public record Stadium(Long id, String fullName, String shortName, String region, Integer externalId, Boolean active,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public String getRegion() {
        return region;
    }

    public Integer getExternalId() {
        return externalId;
    }
}
