package kr.co.victoryfairy.core.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dodn.springboot.core.enums.EventType;
import kr.co.victoryfairy.core.event.model.EventDomain;
import kr.co.victoryfairy.core.event.service.EventService;
import kr.co.victoryfairy.support.handler.RedisHandler;
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

        for (MapRecord<String, Object, Object> message : messages) {
            try {
                var event = objectMapper.convertValue(message.getValue(), EventDomain.WriteEventDto.class);

                if (event.type().equals(EventType.DIARY)) {
                    eventService.processDiary(event);
                } else {
                    eventService.processBatch(event);
                }

                redisHandler.eventKnowEdge(key, group, message.getId().getValue());
            } catch (Exception e) {
                log.error("Error processing diary_written message: {}", message, e);
            }
        }
    }
}
