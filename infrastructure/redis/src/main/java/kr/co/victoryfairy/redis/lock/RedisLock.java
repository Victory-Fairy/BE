package kr.co.victoryfairy.redis.lock;

import org.springframework.util.Assert;

import lombok.Getter;

/**
 * Redis 분산 락 키 생성 객체
 */
@Getter
public class RedisLock {

    private final String lockName;

    private RedisLock(LockName lockName, String key) {
        Assert.hasText(key, "'key' must not be empty");
        this.lockName = lockName.createLockName(key);
    }

    /**
     * RedisLock 인스턴스 생성
     * @param lockName 락 이름 Enum
     * @param key 바인딩할 키 값
     * @return RedisLock 인스턴스
     */
    public static RedisLock from(LockName lockName, String key) {
        return new RedisLock(lockName, key);
    }

}