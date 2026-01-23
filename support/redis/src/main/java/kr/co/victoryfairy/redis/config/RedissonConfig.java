package kr.co.victoryfairy.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

/**
 * Redisson Client 설정
 * <p>
 * Redis 분산 락을 위한 Redisson 클라이언트를 생성합니다.
 */
@Configuration
@RequiredArgsConstructor
public class RedissonConfig {

    private final RedisProperties redisProperties;

    private static final String REDIS_PREFIX = "redis://";

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = REDIS_PREFIX + redisProperties.getHost() + ":" + redisProperties.getPort();

        config.useSingleServer()
            .setAddress(address)
            .setDatabase(redisProperties.getDatabase())
            .setConnectionMinimumIdleSize(1)
            .setConnectionPoolSize(2);

        return Redisson.create(config);
    }

}