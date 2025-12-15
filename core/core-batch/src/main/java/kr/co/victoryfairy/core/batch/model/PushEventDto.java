package kr.co.victoryfairy.core.batch.model;

import io.dodn.springboot.core.enums.MatchEnum;

public record PushEventDto(
        String gameId,
        Long awayId,
        Long homeId,
        MatchEnum.MatchStatus status
) {
}
