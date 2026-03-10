package com.winten.greenlight.admin.domain.actiongroup;

import com.winten.greenlight.admin.db.repository.mapper.actiongroup.ActionGroupMapper;
import com.winten.greenlight.admin.domain.action.Action;
import com.winten.greenlight.admin.domain.action.ActionService;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import com.winten.greenlight.admin.support.util.RedisKeyBuilder;
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
    private final ActionGroupMapper actionGroupMapper;
    private final ActionService actionService;
    private final CachedActionGroupService cachedActionGroupService;
    private final ActionGroupCacheManager actionGroupCacheManager;

    @Transactional(readOnly = true)
    public List<ActionGroup> getAllActionGroup() {
        return actionGroupMapper.findAllActionGroup();
    }

    @Transactional(readOnly = true)
    public ActionGroup getActionGroupById(Long id) {
        ActionGroup actionGroup = ActionGroup.builder()
                                        .id(id)
                                        .build();
        return actionGroupMapper.findOneById(actionGroup)
                .orElseThrow(() -> CoreException.of(ErrorType.ACTION_GROUP_NOT_FOUND, "액션 그룹을 찾을 수 없습니다. ID: " + id));
    }

    @Transactional(readOnly = true)
    public ActionGroup getActionGroupByIdWithAction(Long id) {
        ActionGroup actionGroup = getActionGroupById(id);
        List<Action> actions = actionService.getActionsByGroup(id);
        actionGroup.setActions(actions);
        return actionGroup;
    }


    @Transactional
    public ActionGroup createActionGroup(ActionGroup actionGroup) {
        ActionGroup result = actionGroupMapper.saveActionGroup(actionGroup);

        // Redis put
        actionGroupCacheManager.updateActionGroupMetaCache(result);

        return result;
    }

    @Transactional
    public ActionGroup updateActionGroup(ActionGroup actionGroup) {
        ActionGroup currentActionGroup = getActionGroupById(actionGroup.getId()); // action group 존재여부 확인

        // 본인 Site가 아닐 경우 수정하면 안되므로 검증로직 추가 (SUPER 권한 제외)
        AuthUtil.ensureCanUpdate(currentActionGroup.getUserSiteId());

        ActionGroup result = actionGroupMapper.updateActionGroupById(actionGroup);

        // Redis put
        actionGroupCacheManager.updateActionGroupMetaCache(result);

        // 활성화 상태 변경 시 action 캐시 업데이트
        if (currentActionGroup.getEnabled() != result.getEnabled()) {
            actionService.reloadActionCache();
        }
        return result;
    }

    @Transactional
    public ActionGroup deleteActionGroup(Long id) {
        ActionGroup currentActionGroup = getActionGroupById(id); // action group 존재여부 확인

        // 본인 Site가 아닐 경우 수정하면 안되므로 검증로직 추가 (SUPER 권한 제외)
        AuthUtil.ensureCanDelete(currentActionGroup.getUserSiteId());

        List<Action> actions = actionService.getActionsByGroup(id);

        if (!actions.isEmpty()) {
            throw CoreException.of(ErrorType.NONEMPTY_ACTION_GROUP, "액션 그룹 내에 액션이 존재하여 삭제할 수 없습니다. 액션을 다른 그룹으로 이동하거나 삭제해 주세요.");
        }

        actionGroupMapper.deleteActionGroupById(currentActionGroup);

        // Redis delete
        actionGroupCacheManager.deleteActionGroupMetaCache(currentActionGroup);

        return ActionGroup.builder()
                .id(id)
                .build();
    }

//    public List<ActionGroup> getActionGroupByKey(String greenlightApiKey) {
//        var user = userService.getUserAccountIdByKey(greenlightApiKey);
//        return actionGroupMapper.findAllEnabledWithActions();
//    }

    // action_group:{actionGroup}:queue:WAITING, action_group:{actionGroup}:session의 size 조회
    public List<ActionGroupQueue> getActionGroupQueueStatus() {
        List<ActionGroup> allActionGroups = cachedActionGroupService.getAllActionGroup();

        List<ActionGroup> enabledActionGroups = allActionGroups.stream().filter(ActionGroup::getEnabled).toList();

        List<ActionGroupQueue> result = new ArrayList<>();
        if (enabledActionGroups.isEmpty()) { // enabledActionGroups != null 보장됨
            return result;
        }

        // RTT 절감을 위해 redis 명령어 파이프라이닝
        List<Object> waitingQueueSizes = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (ActionGroup actionGroup : enabledActionGroups) {
                String key = keyBuilder.actionGroupWaitingQueue(actionGroup.getId());
                connection.zSetCommands().zCard(key.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        List<Object> maxTrafficPerSecondList = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (ActionGroup actionGroup : enabledActionGroups) {
                String key = keyBuilder.actionGroupMeta(actionGroup.getId());
                connection.hashCommands().hGet(key.getBytes(StandardCharsets.UTF_8), "maxTrafficPerSecond".getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        for (int i = 0; i < enabledActionGroups.size(); i++) {
            Long id = enabledActionGroups.get(i).getId();
            int waitingSize = 0;
            int estimatedWaitTime = 0;
//            int activeUserCount = 0;
            try {
                waitingSize = Integer.parseInt(waitingQueueSizes.get(i).toString());
//                activeUserCount = Integer.parseInt(activeUserCounts.get(i).toString());
                int maxTrafficPerSecond = Integer.parseInt(maxTrafficPerSecondList.get(i).toString());
                estimatedWaitTime = maxTrafficPerSecond > 0
                        ? Math.round((float) waitingSize / maxTrafficPerSecond)
                        : -1; // -1은 진입불가 
            } catch (Exception e) {
                log.error("[getAllWaitingQueueSize] parsing waiting queue size failed");
            }
            var queue = ActionGroupQueue.builder()
                    .actionGroupId(id)
                    .waitingSize(waitingSize)
                    .estimatedWaitTime(estimatedWaitTime)
//                    .activeUserCount(activeUserCount)
                    .build();
            result.add(queue);
        }

        return result;
    }

    public Long getSessionCount() {
        var key = keyBuilder.session();
        return stringRedisTemplate.opsForZSet().size(key);

    }

    public void reloadActionGroupCache() {
        AuthUtil.ensureSuper();
        List<ActionGroup> actionGroupList = getAllActionGroup();
        for (ActionGroup actionGroup : actionGroupList) {
            actionGroupCacheManager.deleteActionGroupMetaCache(actionGroup);
            if (actionGroup.getEnabled()) {
                actionGroupCacheManager.updateActionGroupMetaCache(actionGroup);
            }
        }
        actionService.reloadActionCache();
    }
}