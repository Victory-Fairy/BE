package kr.co.victoryfairy.core.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.victoryfairy.core.event.model.EventDomain;
import kr.co.victoryfairy.core.event.service.PushService;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PushFcmConsumer {

    Logger log = LoggerFactory.getLogger(PushFcmConsumer.class);

    @Value("${event.fcm.key}")
    private String key;
    @Value("${event.fcm.group}")
    private String group;
    @Value("${event.fcm.consumer}")
    private String consumer;

    private final RedisHandler redisHandler;
    private final ObjectMapper objectMapper;

    private final PushService pushService;

    public PushFcmConsumer(RedisHandler redisHandler, ObjectMapper objectMapper, PushService pushService) {
        this.redisHandler = redisHandler;
        this.objectMapper = objectMapper;
        this.pushService = pushService;
    }

    @Scheduled(fixedDelay = 1000)
    public void consume() {
        List<MapRecord<String, Object, Object>> messages = redisHandler.getEventMessages(key, group, consumer);
        log.info("========== push fcm  Start ==========");

        for (MapRecord<String, Object, Object> message : messages) {
            var event = objectMapper.convertValue(message.getValue(), EventDomain.PushEventDto.class);

            boolean success = pushService.processPushFcm(event);

            /*if (success) {
                redisHandler.eventKnowEdge(key, group, message.getId().getValue());
            } else {
                log.warn("Event processing skipped: {}", message.getId());
            }*/
        }
    }
}
