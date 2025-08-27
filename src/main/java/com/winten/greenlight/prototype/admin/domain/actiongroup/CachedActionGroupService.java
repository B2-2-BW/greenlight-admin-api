package com.winten.greenlight.prototype.admin.domain.actiongroup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.prototype.admin.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CachedActionGroupService {
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisKeyBuilder keyBuilder;
    private final ObjectMapper objectMapper;
    private final RedisTemplate jsonRedisTemplate;

    /**
     * @return actionGroupId : 대시보드 SSE 연결 시에 액션그룹 리스트 조회를 위해 사용
     */
    public List<ActionGroup> getAllActionGroup() {
        String patternKey = keyBuilder.actionGroupKeyPattern();
        Set<String> actionGroupKeys = stringRedisTemplate.keys(patternKey);
        if (actionGroupKeys == null) {
            return List.of();
        }
        List<Object> actionGroups = jsonRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String actionGroupKey : actionGroupKeys) {
                connection.hashCommands().hGetAll(actionGroupKey.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });
        return actionGroups.stream().map(ag -> objectMapper.convertValue(ag, ActionGroup.class)).toList();
    }
}