package com.winten.greenlight.admin.db.repository.redis.dashboard;

import com.winten.greenlight.admin.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DashboardCacheRepository {

    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisTemplate<String, String> stringRedisTemplate;
}