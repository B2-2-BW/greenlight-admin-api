package com.winten.greenlight.prototype.admin.domain.actiongroup;

import com.winten.greenlight.prototype.admin.client.core.CoreClient;
import com.winten.greenlight.prototype.admin.db.repository.mapper.actiongroup.ActionGroupMapper;
import com.winten.greenlight.prototype.admin.db.repository.redis.RedisWriter;
import com.winten.greenlight.prototype.admin.domain.action.Action;
import com.winten.greenlight.prototype.admin.domain.action.ActionService;
import com.winten.greenlight.prototype.admin.domain.user.CurrentUser;
import com.winten.greenlight.prototype.admin.domain.user.UserService;
import com.winten.greenlight.prototype.admin.support.error.CoreException;
import com.winten.greenlight.prototype.admin.support.error.ErrorType;
import com.winten.greenlight.prototype.admin.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionGroupService {
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisKeyBuilder keyBuilder;
    private final RedisWriter redisWriter;
    private final ActionGroupMapper actionGroupMapper;
    private final ActionService actionService;
    private final ActionGroupConverter actionGroupConverter;
    private final UserService userService;
    private final CachedActionGroupService cachedActionGroupService;
    private final CoreClient coreClient;

    public List<ActionGroup> getAllActionGroupByOwnerId(CurrentUser currentUser, ActionGroup actionGroup) {
        actionGroup.setOwnerId(currentUser.getUserId());
        return actionGroupMapper.findAll(actionGroup);
    }

    public ActionGroup getActionGroupById(Long id, CurrentUser currentUser) {
        ActionGroup actionGroup = ActionGroup.builder()
                                        .id(id)
                                        .ownerId(currentUser.getUserId())
                                        .build();
        return actionGroupMapper.findOneById(actionGroup)
                .orElseThrow(() -> CoreException.of(ErrorType.ACTION_GROUP_NOT_FOUND, "액션 그룹을 찾을 수 없습니다. ID: " + id));
    }

    public ActionGroup getActionGroupByIdWithAction(Long id, CurrentUser currentUser) {
        ActionGroup actionGroup = getActionGroupById(id, currentUser);
        List<Action> actions = actionService.getActionsByGroup(id, currentUser);
        actionGroup.setActions(actions);
        return actionGroup;
    }


    @Transactional
    public ActionGroup createActionGroup(ActionGroup actionGroup, CurrentUser currentUser) {
        actionGroup.setOwnerId(currentUser.getUserId());
        ActionGroup result = actionGroupMapper.save(actionGroup);

        // Redis put
        String key = keyBuilder.actionGroupMeta(result.getId());
        redisWriter.putAll(key, actionGroupConverter.toEntity(result));

        // actionGroupKeys Cache 삭제
        cachedActionGroupService.invalidateActionGroupIdsCache();

        return result;
    }

    @Transactional
    public ActionGroup updateActionGroup(ActionGroup actionGroup, CurrentUser currentUser) {
        // TODO currentUser가 ADMIN인 경우 ownerId를 맘대로 변경 가능. 현재는 currentUser의 userId로 강제 입력중
        getActionGroupById(actionGroup.getId(), currentUser); // action group 존재여부 확인
        actionGroup.setOwnerId(currentUser.getUserId());

        ActionGroup result = actionGroupMapper.updateById(actionGroup);

        // Redis put
        String key = keyBuilder.actionGroupMeta(result.getId());
        redisWriter.putAll(key, actionGroupConverter.toEntity(result));

        coreClient.invalidateActionGroupCacheById(actionGroup.getId());

        // actionGroupKeys Cache 삭제
        cachedActionGroupService.invalidateActionGroupIdsCache();
        return result;
    }

    @Transactional
    public ActionGroup deleteActionGroup(Long id, CurrentUser currentUser) {
        ActionGroup actionGroup = getActionGroupById(id, currentUser); // action group 존재여부 확인

        List<Action> actions = actionService.getActionsByGroup(id, currentUser);

        if (!actions.isEmpty()) {
            throw CoreException.of(ErrorType.NONEMPTY_ACTION_GROUP, "액션 그룹 내에 액션이 존재하여 삭제할 수 없습니다. 액션을 다른 그룹으로 이동하거나 삭제해 주세요.");
        }

        actionGroupMapper.deleteById(actionGroup);

        // Redis put
        String key = keyBuilder.actionGroupMeta(id);
        redisWriter.delete(key);

        coreClient.invalidateActionGroupCacheById(actionGroup.getId());

        // actionGroupKeys Cache 삭제
        cachedActionGroupService.invalidateActionGroupIdsCache();

        return ActionGroup.builder()
                .id(id)
                .build();
    }

    public List<ActionGroup> getActionGroupByKey(String greenlightApiKey) {
        var user = userService.getUserAccountIdByKey(greenlightApiKey);
        var actionGroup = ActionGroup.builder()
                .ownerId(user.getUserId())
                .build();
        return actionGroupMapper.findAllEnabledWithActions(actionGroup);
    }

    // action_group:{actionGroup}:queue:WAITING, action_group:{actionGroup}:session의 size 조회
    public List<ActionGroupQueue> getActionGroupQueueStatus(CurrentUser currentUser) {
        List<Long> actionGroupIds = cachedActionGroupService.getActionGroupIds(currentUser);

        List<ActionGroupQueue> result = new ArrayList<>();
        if (actionGroupIds == null || actionGroupIds.isEmpty()) {
            return result;
        }

        // RTT 절감을 위해 redis 명령어 파이프라이닝
        List<Object> waitingQueueSizes = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long actionGroupId : actionGroupIds) {
                String key = keyBuilder.actionGroupWaitingQueue(actionGroupId);
                connection.zSetCommands().zCard(key.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        List<Object> maxActiveCustomerList = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long actionGroupId : actionGroupIds) {
                String key = keyBuilder.actionGroupMeta(actionGroupId);
                connection.hashCommands().hGet(key.getBytes(StandardCharsets.UTF_8), "maxActiveCustomers".getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        List<Object> sessionSizes = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long actionGroupId : actionGroupIds) {
                String key = keyBuilder.actionGroupSession(actionGroupId);
                connection.zSetCommands().zCard(key.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        for (int i = 0; i < actionGroupIds.size(); i++) {
            Long id = actionGroupIds.get(i);
            int waitingSize = 0;
            int sessionSize = 0;
            int estimatedWaitTime = 0;
            try {
                waitingSize = Integer.parseInt(waitingQueueSizes.get(i).toString());
                sessionSize = Integer.parseInt(sessionSizes.get(i).toString());
                int maxActiveCustomers = Integer.parseInt(maxActiveCustomerList.get(i).toString());
                estimatedWaitTime = maxActiveCustomers > 0
                        ? Math.round((float) waitingSize / maxActiveCustomers)
                        : 0;
            } catch (Exception e) {
                log.error("[getAllWaitingQueueSize] parsing waiting queue size failed");
            }
            var queue = new ActionGroupQueue(id, waitingSize, sessionSize, estimatedWaitTime);
            result.add(queue);
        }

        return result;
    }
}