package com.winten.greenlight.admin.domain.actiongroup;

import com.winten.greenlight.admin.db.repository.redis.RedisWriter;
import com.winten.greenlight.admin.domain.action.ActionCacheManager;
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
    private final ActionCacheManager actionCacheManager;

    public void updateActionGroupMetaCache(final ActionGroup actionGroup) {
        String key = redisKeyBuilder.actionGroupMeta(actionGroup.getId());
        redisWriter.putAll(key, actionGroupConverter.toEntity(actionGroup));

        actionCacheManager.updateActionVersion();
    }

    public void deleteActionGroupMetaCache(final ActionGroup actionGroup) {
        String key = redisKeyBuilder.actionGroupMeta(actionGroup.getId());
        redisWriter.delete(key);

        actionCacheManager.updateActionVersion();
    }
}