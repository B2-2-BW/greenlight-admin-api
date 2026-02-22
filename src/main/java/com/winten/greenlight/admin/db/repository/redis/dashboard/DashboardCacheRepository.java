package com.winten.greenlight.admin.db.repository.redis.dashboard;

import com.winten.greenlight.admin.domain.customer.WaitStatus;
import com.winten.greenlight.admin.domain.dashboard.RoomStatus;
import com.winten.greenlight.admin.support.util.RedisKeyBuilder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DashboardCacheRepository {

    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisTemplate<String, String> redisTemplate;

    private static @NonNull DefaultRedisScript<List> getRoomStatusCountsScript() {
        String script = """
            local waitingCount      = redis.call('ZCARD',  KEYS[1])
            local activeCustomerCount = redis.call('ZCARD',  KEYS[2])
            local enteredRate       = redis.call('ZCOUNT', KEYS[3], ARGV[1], ARGV[2])
            local inflowRate        = redis.call('ZCOUNT', KEYS[4], ARGV[1], ARGV[2])
            local outflowRate       = redis.call('ZCOUNT', KEYS[5], ARGV[1], ARGV[2])
            return { waitingCount, activeCustomerCount, enteredRate, inflowRate, outflowRate }
        """;
        var redisScript = new DefaultRedisScript<List>();
        redisScript.setScriptText(script);
        // Spring Data Redis는 resultType으로 List.class 등을 사용해 스크립트 결과를 변환합니다.
        redisScript.setResultType(List.class); // List<Long>로 들어오는 형태를 기대 [web:20]
        return redisScript;
        }

    // TODO Influx DB 또는 Cache를 쓰도록 개선해야함
    //  현재 API 동시호출 시 성능이슈 존재함
    public RoomStatus getRoomStatus(String roomId, long windowSeconds) {
        List<String> keys = List.of(
            redisKeyBuilder.roomQueue(roomId, WaitStatus.WAITING), // Sorted Set. room:{roomId}:queue:WAITING. 전체 size() 반환
            redisKeyBuilder.roomHeartbeat(roomId, WaitStatus.ENTERED), // Sorted Set. room:{roomId}:heartbeat:ENTERED. 전체 size() 반환tKey,
            redisKeyBuilder.roomQueue(roomId, WaitStatus.ENTERED), // Sorted Set. room:{roomId}:queue:ENTERED ranged size. start, end 사이의 size 반환
            redisKeyBuilder.roomMetricInflow(roomId), // Sorted Set. room:{roomId}:metrics:inflow. start, end 사이의 size 반환
            redisKeyBuilder.roomMetricOutflow(roomId) // Sorted Set. room:{roomId}:metrics:outflow. start, end 사이의 size 반환
        );

        var endScore = System.currentTimeMillis();
        var startScore = endScore - (windowSeconds * 1000);

        List result = redisTemplate.execute(
                getRoomStatusCountsScript(),
                keys,
                String.valueOf(startScore),
                String.valueOf(endScore)
        );

        long waitingCount = ((Number) result.get(0)).longValue();
        long activeCustomerCount = ((Number) result.get(1)).longValue();
        long entered = ((Number) result.get(2)).longValue();
        long inflow = ((Number) result.get(3)).longValue();
        long outflow = ((Number) result.get(4)).longValue();

        return RoomStatus.builder()
                .roomId(roomId)
                .waitingCount(waitingCount)
                .activeCustomerCount(activeCustomerCount)
                .entered(entered)
                .inflow(inflow)
                .outflow(outflow)
                .build();
    }
}