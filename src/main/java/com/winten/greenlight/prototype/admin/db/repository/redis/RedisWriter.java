package com.winten.greenlight.prototype.admin.db.repository.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.prototype.admin.support.dto.Hashable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RedisWriter {
    private final RedisTemplate<String, Object> jsonRedisTemplate;
    private final ObjectMapper objectMapper;

    public void putAll(String key, Hashable dto) {
        Map<String, Object> map =
                objectMapper.convertValue(dto, new TypeReference<>() {}); // DTO to Map 변환
        jsonRedisTemplate.opsForHash().putAll(key, map);
    }

    public void delete(String key) {
        jsonRedisTemplate.delete(key);
    }

}