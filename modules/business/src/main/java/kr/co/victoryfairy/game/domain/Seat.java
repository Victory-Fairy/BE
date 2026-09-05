package kr.co.victoryfairy.game.domain;

import java.time.LocalDateTime;

public record Seat(Long id, Long stadiumId, String name, String season, Boolean active, LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
