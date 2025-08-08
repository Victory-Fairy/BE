package kr.co.victoryfairy.core.event.config;

import io.lettuce.core.RedisBusyException;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;

@Configuration
public class Initializer {

    Logger log = LoggerFactory.getLogger(Initializer.class);

    @Value("${event.steam.key}")
    private String key;
    @Value("${event.steam.group}")
    private String group;

    @Bean
    public InitializingBean initializeConsumerGroup(RedisHandler redisHandler) {
        return () -> {
            try {
                redisHandler.initEvent(key, group);
            } catch (RedisSystemException e) {
                if (e.getCause() instanceof RedisBusyException busy &&
                        busy.getMessage().contains("BUSYGROUP")) {
                    log.warn("Consumer group already exists: {}", group);
                } else {
                    throw e;
                }
            }
        };
    }

}
