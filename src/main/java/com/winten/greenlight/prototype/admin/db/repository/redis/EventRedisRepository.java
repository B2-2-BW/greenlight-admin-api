package com.winten.greenlight.prototype.admin.db.repository.redis;

import com.winten.greenlight.prototype.admin.domain.event.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventRedisRepository {
    private final String KEY_PREFIX = "proto-event:";
    private final RedisTemplate<String, EventEntity> redisTemplate;

    public void put(Event event) {
        redisTemplate.opsForValue().set(KEY_PREFIX + event.getEventName(), event.toEntity());
    }

    public void pop(String eventName) {
        redisTemplate.delete(KEY_PREFIX + eventName);
    }
}