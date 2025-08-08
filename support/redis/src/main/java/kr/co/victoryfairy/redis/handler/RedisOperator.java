package kr.co.victoryfairy.redis.handler;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisOperator {
    private final RedisTemplate<String, String> redisTemplate;

    public PendingMessagesSummary pendingSummary(String streamKey, String groupName) {
        return redisTemplate.opsForStream().pending(streamKey, groupName);
    }

    public List<PendingMessage> getPendingDetails(String streamKey, String groupName, int count) {
        var connection = redisTemplate.getConnectionFactory().getConnection();
        var streamCommands = connection.streamCommands();

        var pendingMessages = streamCommands.xPending(
            streamKey.getBytes(),
            groupName,
            Range.unbounded(),
            Long.valueOf(count)
        );

        return pendingMessages.stream().toList();
    }

    public List<PendingMessage> getPendingDetailsPaged(String streamKey, String groupName, int pageSize) {
        var connection = redisTemplate.getConnectionFactory().getConnection();
        var streamCommands = connection.streamCommands();

        String startId = "0-0";
        String endId = "+";
        List<PendingMessage> allMessages = new ArrayList<>();

        while (true) {
            // 조회 범위 설정
            var range = Range.closed(startId, endId);

            // 메시지 조회
            List<PendingMessage> messages = streamCommands.xPending(
                    streamKey.getBytes(),
                    groupName,
                    range,
                    (long) pageSize
            ).toList();

            if (messages.isEmpty()) break;

            allMessages.addAll(messages);

            // 다음 조회 시작 ID 설정 (마지막 메시지 ID + 1)
            String lastId = messages.get(messages.size() - 1).getIdAsString();
            startId = incrementStreamId(lastId);

            // 페이징이 끝났는지 확인
            if (messages.size() < pageSize) break;
        }

        return allMessages;
    }

    public List<MapRecord<String, Object, Object>> read(String streamKey, String groupName, String consumerName, String id) {
        return redisTemplate.opsForStream().read(
                Consumer.from(groupName, consumerName),
                StreamReadOptions.empty().count(1),
                StreamOffset.create(streamKey, ReadOffset.from(RecordId.of(id)))
        );
    }

    public void ack(String streamKey, String groupName, String id) {
        redisTemplate.opsForStream().acknowledge(streamKey, groupName, RecordId.of(id));
    }

    public List<MapRecord<String, Object, Object>> readAll(String streamKey, String groupName, String consumerName, int count) {
        return redisTemplate.opsForStream().read(
                Consumer.from(groupName, consumerName),
                StreamReadOptions.empty().count(count),
                StreamOffset.create(streamKey, ReadOffset.from("0")) // 처음부터 읽기 (ack 여부 상관없음)
        );
    }

    public List<MapRecord<String, Object, Object>> rangeAll(String streamKey) {
        return redisTemplate.opsForStream().range(
                streamKey,
                Range.unbounded()
        );
    }

    private String incrementStreamId(String id) {
        String[] parts = id.split("-");
        long timestamp = Long.parseLong(parts[0]);
        long sequence = Long.parseLong(parts[1]);
        return timestamp + "-" + (sequence + 1);
    }
}
