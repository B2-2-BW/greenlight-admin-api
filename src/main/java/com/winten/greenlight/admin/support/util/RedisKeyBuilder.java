package com.winten.greenlight.admin.support.util;

import com.winten.greenlight.admin.domain.customer.WaitStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisKeyBuilder {
    @Value("${redis.key-prefix}")
    private String prefix;

    public String actionGroupKeyPattern() {
        return prefix + ":action_group:*:meta";
    }

    // TODO 한눈에 보기 쉽게 완성된 full string을 주석에 추가하기
    public String actionGroupMeta(Long actionGroupId) {
        return prefix + ":action_group:" + actionGroupId + ":meta";
    }

    public String actionGroupStatus(Long actionGroupId) {
        return prefix + ":action_group:" + actionGroupId + ":status";
    }

    public String action() {
        return prefix + ":action:*";
    }

    public String action(Long actionId) {
        return prefix + ":action:" + actionId;
    }

    public String landingCacheKey(String landingId) {
        return prefix + ":landing_action_mapping:" + landingId;
    }

    public String actionVersion() {
        return prefix + ":api:action:version";
    }

    public String userApiKey() {
        return prefix + ":admin:user_api_key";
    }

    public String allActions() {
        return prefix + ":action:*";
    }

    public String actionEventStream() {
        return prefix + ":infra:action_event:stream";
    }
    public String actionEventDlqStream() {
        return prefix + ":infra:action_event:dlq";
    }

    public String actionGroupWaitingQueue(Long actionGroupId) {
        return prefix + ":action_group:" + actionGroupId + ":queue:WAITING";
    }

    public String session() {
        return prefix + ":session";
    }

    public String actionGroupAccessLog(Long actionGroupId) {
        return prefix + ":action_group:" + actionGroupId + ":accesslog";
    }

    public String urlCachingKey(String url) {
        return prefix + ":url_action_mapping:" + url;
    }

    public String roomMeta(String roomId) {
        return prefix + ":room:" + roomId + ":meta";
    }

    public String heartbeat(String roomId, WaitStatus waitStatus) {
        return prefix + ":room:" + roomId + ":heartbeat:" + waitStatus.name();
    }

    public String siteApiKey(String apiKey) {
        return prefix + ":site:key_id_mapping:" + apiKey;
    }
}