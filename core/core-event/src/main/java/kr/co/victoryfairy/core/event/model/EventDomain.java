package kr.co.victoryfairy.core.event.model;

import io.dodn.springboot.core.enums.EventType;
import io.dodn.springboot.core.enums.MatchEnum;

public interface EventDomain {

    record WriteEventDto(String gameId, Long memberId, Long diaryId, EventType type) {
    }

    public record PushEventDto(String gameId, Long awayId, Long homeId, MatchEnum.MatchStatus status) {
    }

}
