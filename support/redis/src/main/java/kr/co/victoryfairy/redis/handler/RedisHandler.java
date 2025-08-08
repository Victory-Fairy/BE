package kr.co.victoryfairy.redis.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RedisHandler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private Logger log = LoggerFactory.getLogger(RedisHandler.class);

    public void setString(String key, String value) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(key, value);
        System.out.println(valueOperations.get(key));
        redisTemplate.convertAndSend("", "dfdf");
    }

    public void setMap(String key, Map<String, String> map) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        hashOperations.putAll(key, map);
        /*map.entrySet().forEach(entry -> {
            hashOperations.put(key, entry.getKey(), entry.getValue());
        });*/
    }

    public void put(String hashKey, String key, Object value) {
        redisTemplate.opsForHash().put(hashKey, key, value);
    }

    public String get(String key, String hashKey) {
        return (String) redisTemplate.opsForHash().get(key, hashKey);
    }

    public String get(String key) {
        Object o = redisTemplate.opsForValue().get(key);
        if (o == null) {
            return null;
        }
        return (String) o;
    }

    public Map<String, String> getMap(String key) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        return hashOperations.entries(key);
    }

    public void delete(String key, String hashKey) {
        redisTemplate.opsForHash().delete(key, hashKey);
    }

    public void deleteMap(String key) {
        redisTemplate.delete(key); // 전체 삭제
    }

    /**
     * hash 구조로 저장된 데이터에서 key 값으로 데이터 조회
     */
    public String getHashValue(String hashKey, String key) {
        return get(hashKey, key);
    }

    /**
     * hash 구조로 저장된 데이터에서 key 값으로 역직렬화 해서 조회
     */
    public <T> T getHashValue(String hashKey, String key, Class<T> clazz) {
        Object json = redisTemplate.opsForHash().get(hashKey, key);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json.toString(), clazz);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new CustomException(MessageEnum.Common.REQUEST_FAIL);
        }
    }

    /**
     * hash 구조에 객체 Json 직렬화 저장
     */
    public void setHashValue(String hashKey, String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForHash().put(hashKey, key, json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new CustomException(MessageEnum.Common.REQUEST_FAIL);
        }
    }

    /**
     * hash 데이터 삭제
     */
    public void deleteHashValue(String hashKey, String key) {
        redisTemplate.opsForHash().delete(hashKey, key);
    }

    /**
     * Redis Stream pub
     */
    public void pushEvent(String channel, Object obj) {
        try {
            Map<String, String> map = objectMapper.convertValue(obj, new TypeReference<>() {});
            redisTemplate.opsForStream().add(channel, map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initEvent(String key, String groupName) {
        redisTemplate.opsForStream().createGroup(key, ReadOffset.latest(), groupName);
    }

    public List<MapRecord<String, Object, Object>> getEventMessages(String key, String groupName, String consumer) {
        return redisTemplate.opsForStream()
                .read(Consumer.from(groupName, consumer),
                        StreamReadOptions.empty().block(Duration.ofSeconds(2)).count(10),
                        StreamOffset.create(key, ReadOffset.lastConsumed()));
    }

    public void eventKnowEdge(String key, String groupName, String recordId) {
        Long acked = redisTemplate.opsForStream().acknowledge(key, groupName, recordId);
        Long deleted = redisTemplate.opsForStream().delete(key, recordId);

        log.info(">> eventKnowEdge: ack={}, delete={}", acked, deleted);
    }

    public void pushHash(String key, String id, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForHash().put(key, id, json); // 덮어쓰기
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis JSON 직렬화 실패", e);
        }
    }

    public void deleteHash(String key, String id) {
        redisTemplate.opsForHash().delete(key, id);
    }

    public void deleteHash(String key) {
        redisTemplate.delete(key);
    }

    public Map<String, Map<String, Object>> getHashMap(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<String, Map<String, Object>> result = new HashMap<>();

        entries.forEach((rKey, value) -> {
            try {
                String id = rKey.toString();
                Map<String, Object> matchData = objectMapper.readValue(value.toString(), new TypeReference<>() {});
                result.put(id, matchData);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Redis JSON 역직렬화 실패", e);
            }
        });

        return result;
    }

    public Map<String, List<Object>> getHashMapList(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<String, List<Object>> result = new HashMap<>();

        entries.forEach((rKey, value) -> {
            try {
                String id = rKey.toString();
                List<Object> matchData = objectMapper.readValue(value.toString(), new TypeReference<>() {});
                result.put(id, matchData);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Redis JSON 역직렬화 실패", e);
            }
        });

        return result;
    }

    /**
     * 락 시도
     */
    public boolean tryLock(String key, Duration timeout) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "lock", timeout);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 락 해제
     */
    public void releaseLock(String key) {
        redisTemplate.delete(key);
    }

}