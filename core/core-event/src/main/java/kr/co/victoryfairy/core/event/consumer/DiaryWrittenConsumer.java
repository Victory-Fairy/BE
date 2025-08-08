package kr.co.victoryfairy.core.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.victoryfairy.core.event.model.EventDomain;
import kr.co.victoryfairy.core.event.service.EventService;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiaryWrittenConsumer {

    Logger log = LoggerFactory.getLogger(DiaryWrittenConsumer.class);

    @Value("${event.steam.key}")
    private String key;
    @Value("${event.steam.group}")
    private String group;
    @Value("${event.steam.consumer}")
    private String consumer;

    private final RedisHandler redisHandler;
    private final ObjectMapper objectMapper;

    private final EventService eventService;

    public DiaryWrittenConsumer(ObjectMapper objectMapper, RedisHandler redisHandler, EventService eventService) {
        this.objectMapper = objectMapper;
        this.redisHandler = redisHandler;
        this.eventService = eventService;
    }

    @Scheduled(fixedDelay = 1000)
    public void consume() {
        List<MapRecord<String, Object, Object>> messages = redisHandler.getEventMessages(key, group, consumer);
        log.info("========== event  Start ==========");
        for (MapRecord<String, Object, Object> message : messages) {
            try {
                var event = objectMapper.convertValue(message.getValue(), EventDomain.WriteEventDto.class);

                boolean success = switch (event.type()) {
                    case DIARY -> eventService.processDiary(event);
                    default -> eventService.processBatch(event);
                };

                if (success) {
                    redisHandler.eventKnowEdge(key, group, message.getId().getValue());
                } else {
                    log.warn("Event processing skipped: {}", message.getId());
                }
            } catch (Exception e) {
                log.error("Error processing diary_written message: {}", message, e);
            }
        }
    }
}
