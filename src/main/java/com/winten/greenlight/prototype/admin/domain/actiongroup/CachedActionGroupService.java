package com.winten.greenlight.prototype.admin.domain.actiongroup;

import com.winten.greenlight.prototype.admin.db.repository.mapper.actiongroup.ActionGroupMapper;
import com.winten.greenlight.prototype.admin.domain.user.CurrentUser;
import com.winten.greenlight.prototype.admin.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CachedActionGroupService {
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisKeyBuilder keyBuilder;
    private final ActionGroupMapper actionGroupMapper;

    /**
     * @return actionGroupId : Redis key 형식의 Map 반환
     */
    @Cacheable(cacheNames = "actionGroupIds")
    public List<Long> getActionGroupIds(CurrentUser currentUser) {
        var entity = ActionGroup.builder()
                .ownerId(currentUser.getUserId())
                .build();
        var actionGroups = actionGroupMapper.findAll(entity);
        return actionGroups.stream().map(ActionGroup::getId).collect(Collectors.toList());
    }

    @CacheEvict(cacheNames = "actionGroupIds")
    public void invalidateActionGroupIdsCache() {}
}