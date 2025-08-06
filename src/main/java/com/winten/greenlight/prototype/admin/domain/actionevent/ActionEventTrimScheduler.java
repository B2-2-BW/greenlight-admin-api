package com.winten.greenlight.prototype.admin.domain.actionevent;

import com.winten.greenlight.prototype.admin.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ActionEventTrimScheduler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyBuilder keyBuilder;

    @Scheduled(fixedRate = 60000)
    public void trimStream() {
        redisTemplate.opsForStream().trim(keyBuilder.actionEventStream(), 100_000);
    }
}