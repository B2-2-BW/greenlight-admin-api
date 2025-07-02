package com.winten.greenlight.prototype.admin.domain.actiongroup;

import com.winten.greenlight.prototype.admin.db.repository.mapper.action.ActionGroupMapper;
import com.winten.greenlight.prototype.admin.db.repository.redis.RedisWriter;
import com.winten.greenlight.prototype.admin.domain.user.CurrentUser;
import com.winten.greenlight.prototype.admin.support.error.CoreException;
import com.winten.greenlight.prototype.admin.support.error.ErrorType;
import com.winten.greenlight.prototype.admin.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActionGroupService {
    private final ActionGroupMapper actionGroupMapper;
    private final RedisKeyBuilder keyBuilder;
    private final RedisWriter redisWriter;

    public List<ActionGroup> getAllActionGroupByOwnerId(CurrentUser currentUser) {
        return actionGroupMapper.findAll(currentUser.getUserId());
    }

    public ActionGroup getActionGroupById(Long id, CurrentUser currentUser) {
        ActionGroup actionGroup = ActionGroup.builder()
                                        .id(id)
                                        .ownerId(currentUser.getUserId())
                                        .build();
        return actionGroupMapper.findOneById(actionGroup)
                .orElseThrow(() -> CoreException.of(ErrorType.ACTION_GROUP_NOT_FOUND, "액션 그룹을 찾을 수 없습니다. ID: " + id));
    }

    @Transactional
    public ActionGroup createActionGroup(ActionGroup actionGroup, CurrentUser currentUser) {
        actionGroup.setOwnerId(currentUser.getUserId());
        ActionGroup result = actionGroupMapper.save(actionGroup);

        // Redis put
        String key = keyBuilder.actionGroupMeta(result.getId());
        redisWriter.putAll(key, result);

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
        redisWriter.putAll(key, result);

        return result;
    }

    @Transactional
    public ActionGroup deleteActionGroup(Long id, CurrentUser currentUser) {
        ActionGroup actionGroup = getActionGroupById(id, currentUser); // action group 존재여부 확인
        actionGroupMapper.deleteById(actionGroup);

        // Redis put
        String key = keyBuilder.actionGroupMeta(id);
        redisWriter.delete(key);

        return ActionGroup.builder()
                .id(id)
                .build();
    }
}