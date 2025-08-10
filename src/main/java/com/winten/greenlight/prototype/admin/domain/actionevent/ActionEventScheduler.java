package com.winten.greenlight.prototype.admin.domain.actionevent;

import com.influxdb.client.WriteApi;
import com.influxdb.client.write.Point;
import com.winten.greenlight.prototype.admin.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ActionEventScheduler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyBuilder keyBuilder;
    private final WriteApi writeApi;
    private final ActionEventQueue actionEventQueue;

    private final int MAX_DRAIN = 10000;

    @Scheduled(fixedRate = 60000)
    public void trimStream() {
        try {
            redisTemplate.opsForStream().trim(keyBuilder.actionEventStream(), 100_000);
        } catch (Exception e) {
            log.error("failed to trim stream", e);
        }
    }

    // TODO 스케쥴링으로 진행 시,
    @Scheduled(fixedRate = 1000)
    public void writePoints() {
        try {
            int size = actionEventQueue.size();
            if (size == 0) return;

            List<Point> points = new ArrayList<>(Math.min(size, MAX_DRAIN));
            actionEventQueue.drainTo(points, MAX_DRAIN);
            if (points.isEmpty()) return;
            writeApi.writePoints(points);
        } catch (Exception e) {
            log.error("failed to write points", e);
        }
    }
}