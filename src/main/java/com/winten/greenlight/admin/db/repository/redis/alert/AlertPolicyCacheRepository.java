package com.winten.greenlight.admin.db.repository.redis.alert;

import com.winten.greenlight.admin.domain.alert.AlertPolicy;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.RedisKeyBuilder;
import io.lettuce.core.RedisException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AlertPolicyCacheRepository {
    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisTemplate<String, Object> jsonRedisTemplate;

    public void updateAlertPolicy(AlertPolicy policy) {
        if (policy == null || policy.getSiteId() == null || policy.getSiteId().isBlank()) {
            throw CoreException.of(ErrorType.INVALID_DATA, "사이트 ID가 필요합니다.");
        }
        String key = redisKeyBuilder.alertPolicy(policy.getSiteId());
        var values = new HashMap<String, Object>();
        values.put("forTicks", policy.getForTicks());
        values.put("repeatIntervalSeconds", policy.getRepeatIntervalSeconds());
        values.put("waitingCompare", policy.getWaitingCompare().name());
        values.put("waitingThreshold", policy.getWaitingThreshold());
        values.put("activeCompare", policy.getActiveCompare().name());
        values.put("activeThreshold", policy.getActiveThreshold());
        values.put("surgeWaitTimeSeconds", policy.getSurgeWaitTimeSeconds());
        try {
            jsonRedisTemplate.opsForHash().putAll(key, values);
        } catch (RedisException exception) {
            log.error(exception.getMessage());
            throw new CoreException(ErrorType.REDIS_ERROR, exception);
        }
    }

    public void deleteLegacyKey() {
        try {
            jsonRedisTemplate.delete(redisKeyBuilder.alertPolicyLegacy());
        } catch (RedisException exception) {
            log.error(exception.getMessage());
            throw new CoreException(ErrorType.REDIS_ERROR, exception);
        }
    }
}
