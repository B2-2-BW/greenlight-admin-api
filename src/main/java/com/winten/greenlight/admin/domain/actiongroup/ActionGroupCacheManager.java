package com.winten.greenlight.admin.domain.actiongroup;

import com.winten.greenlight.admin.db.repository.redis.RedisWriter;
import com.winten.greenlight.admin.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActionGroupCacheManager {
    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisWriter redisWriter;
    private final ActionGroupConverter actionGroupConverter;

    public void updateActionGroupMetaCache(final ActionGroup actionGroup) {
        String key = redisKeyBuilder.actionGroupMeta(actionGroup.getId());
        redisWriter.putAll(key, actionGroupConverter.toEntity(actionGroup));
    }

    public void deleteActionGroupMetaCache(final ActionGroup actionGroup) {
        String key = redisKeyBuilder.actionGroupMeta(actionGroup.getId());
        redisWriter.delete(key);
    }
}