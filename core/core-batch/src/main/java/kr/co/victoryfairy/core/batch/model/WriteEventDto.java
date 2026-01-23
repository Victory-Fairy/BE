package kr.co.victoryfairy.core.batch.model;

import io.dodn.springboot.core.enums.EventType;

public record WriteEventDto(String gameId, Long memberId, Long diaryId, EventType type) {
}
