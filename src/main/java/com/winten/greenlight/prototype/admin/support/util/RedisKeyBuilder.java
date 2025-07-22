package com.winten.greenlight.prototype.admin.support.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisKeyBuilder {
    @Value("${redis.key-prefix}")
    private String prefix;

    public String actionGroupMeta(Long actionGroupId) {
        return String.format("%s:action_group:%d:meta", prefix, actionGroupId);
    }

    public String actionGroupStatus(Long actionGroupId) {
        return String.format("%s:action_group:%d:status", prefix, actionGroupId);
    }

    public String action(Long actionId) {
        return String.format("%s:action:%d", prefix, actionId);
    }
}