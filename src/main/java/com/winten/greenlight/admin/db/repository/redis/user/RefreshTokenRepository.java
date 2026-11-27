package com.winten.greenlight.admin.db.repository.redis.user;

import com.winten.greenlight.admin.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {
    private static final DefaultRedisScript<Long> ROTATE = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end "
                    + "if not redis.call('SET', KEYS[2], ARGV[2], 'NX') then return 0 end "
                    + "redis.call('PEXPIREAT', KEYS[2], ARGV[3]) "
                    + "redis.call('DEL', KEYS[1]) return 1", Long.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyBuilder redisKeyBuilder;

    public void save(Long accountId, String tokenId, Instant expiresAt) {
        String key = redisKeyBuilder.refreshToken(accountId, tokenId);
        Boolean saved = redisTemplate.opsForValue().setIfAbsent(key, tokenId,
                java.time.Duration.between(Instant.now(), expiresAt));
        if (!Boolean.TRUE.equals(saved)) {
            throw new IllegalStateException("Could not save refresh token");
        }
    }

    public boolean exists(Long accountId, String tokenId) {
        return tokenId.equals(redisTemplate.opsForValue().get(redisKeyBuilder.refreshToken(accountId, tokenId)));
    }

    public boolean rotate(Long accountId, String oldId, String newId, Instant expiresAt) {
        Long result = redisTemplate.execute(ROTATE, List.of(
                redisKeyBuilder.refreshToken(accountId, oldId),
                redisKeyBuilder.refreshToken(accountId, newId)
        ), oldId, newId, Long.toString(expiresAt.toEpochMilli()));
        return Long.valueOf(1).equals(result);
    }

    public void delete(Long accountId, String tokenId) {
        redisTemplate.delete(redisKeyBuilder.refreshToken(accountId, tokenId));
    }
}
