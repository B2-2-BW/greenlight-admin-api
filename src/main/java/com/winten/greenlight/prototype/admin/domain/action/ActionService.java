package com.winten.greenlight.prototype.admin.domain.action;

import com.winten.greenlight.prototype.admin.db.repository.mapper.action.ActionMapper;
import com.winten.greenlight.prototype.admin.db.repository.mapper.action.ActionRuleMapper;
import com.winten.greenlight.prototype.admin.db.repository.redis.RedisWriter;
import com.winten.greenlight.prototype.admin.domain.user.CurrentUser;
import com.winten.greenlight.prototype.admin.support.error.CoreException;
import com.winten.greenlight.prototype.admin.support.error.ErrorType;
import com.winten.greenlight.prototype.admin.support.util.RedisKeyBuilder;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActionService {
    private final ActionMapper actionMapper;
    private final RedisKeyBuilder keyBuilder;
    private final RedisWriter redisWriter;
    private final ActionRuleMapper actionRuleMapper;

    // TODO Action Rule 추가하기
    public List<Action> getAllActionsByOwnerId(String ownerId) {
        return actionMapper.findAll(ownerId);
    }

    public Action getActionById(Long id, CurrentUser currentUser) {
        Action action = Action.builder()
                .id(id)
                .ownerId(currentUser.getUserId())
                .build();
        return actionMapper.findOneById(action)
                .orElseThrow(() -> CoreException.of(ErrorType.ACTION_NOT_FOUND, "액션을 찾을 수 없습니다. ID: " + id));
    }

    public Action getActionByIdWithRules(Long id, CurrentUser currentUser) {
        Action action = getActionById(id, currentUser);
        List<ActionRule> actionRules = actionRuleMapper.findAllByActionId(id);
        action.setActionRules(actionRules);
        return action;
    }

    public List<Action> getActionsByGroup(Long actionGroupId, CurrentUser currentUser) {
        Action action = Action.builder()
                .actionGroupId(actionGroupId)
                .ownerId(currentUser.getUserId())
                .build();
        action.setOwnerId(currentUser.getUserId());
        return actionMapper.findAllByGroupId(action);
    }

    @Transactional
    public Action createActionInGroup(
            Long actionGroupId,
            Action action,
            List<ActionRule> actionRules,
            CurrentUser currentUser
    ) {
        // DB Insert
        action.setActionGroupId(actionGroupId);
        action.setOwnerId(currentUser.getUserId());
        action.setLandingId(TSID.fast().toString()); // actionType과 관계없이 고유한 LandingId 부여

        validateActionType(action); // actionType 검증

        // Action 저장
        Action actionResult = actionMapper.save(action);

        // Action Rule 저장
        for (ActionRule actionRule : actionRules) {
            actionRule.setActionId(actionResult.getId());
            actionRule.setCreatedBy(currentUser.getUserId());
            actionRule.setUpdatedBy(currentUser.getUserId());
            actionRuleMapper.save(actionRule);
        }
        List<ActionRule> actionRuleResult = actionRuleMapper.findAllByActionId(actionResult.getId());
        actionResult.setActionRules(actionRuleResult);

        // Redis put
        String key = keyBuilder.action(actionGroupId, actionResult.getId());
        redisWriter.putAll(key, actionResult);
        
        return actionResult;
    }

    public Action updateActionById(
            Action action,
            List<ActionRule> actionRules,
            CurrentUser currentUser
    ) {
        getActionById(action.getId(), currentUser); // 존재여부 확인, 없으면 exception
        action.setOwnerId(currentUser.getUserId());

        validateActionType(action); // actionType 검증
        
        // DB Update
        Action actionResult = actionMapper.updateById(action);
        
        // TODO AWS처럼 action rule 개별 업데이트 및 삭제가 가능해야할수도?
        //  현재는 전체 삭제 후 다시 insert 중임
        // Action Rule 삭제 후 저장
        actionRuleMapper.deleteAllByActionId(actionResult.getId());
        for (ActionRule actionRule : actionRules) {
            actionRule.setActionId(actionResult.getId());
            actionRule.setCreatedBy(currentUser.getUserId());
            actionRule.setUpdatedBy(currentUser.getUserId());
            actionRuleMapper.save(actionRule);
        }
        List<ActionRule> actionRuleResult = actionRuleMapper.findAllByActionId(actionResult.getId());
        actionResult.setActionRules(actionRuleResult);

        // Redis put
        String key = keyBuilder.action(actionResult.getActionGroupId(), actionResult.getId());
        redisWriter.putAll(key, actionResult);

        return actionResult;
    }

    public Action deleteActionById(Long actionId, CurrentUser currentUser) {
        Action action = getActionById(actionId, currentUser); // 존재여부 확인, 없으면 exception

        // DB Delete
        actionMapper.deleteById(action);

        actionRuleMapper.deleteAllByActionId(actionId);

        // Redis Delete
        String key = keyBuilder.action(action.getActionGroupId(), actionId);
        redisWriter.delete(key);

        return Action.builder()
                .id(actionId)
                .build();
    }

    private void validateActionType(Action action) {
        if (action.getActionType() == ActionType.LANDING) {
            if (action.getLandingDestinationUrl() == null || action.getLandingDestinationUrl().isEmpty()) {
                throw CoreException.of(ErrorType.INVALID_DATA, "액션유형이 LANDING인 경우 랜딩 목적지(landingDestinationUrl)는 필수로 입력되어야 합니다.");
            }
            if (action.getLandingStartAt() == null) {
                throw CoreException.of(ErrorType.INVALID_DATA, "액션유형이 LANDING인 경우 랜딩 시작시간(landingStartAt)은 필수로 입력되어야 합니다.");
            }
            if (action.getLandingEndAt() == null) {
                throw CoreException.of(ErrorType.INVALID_DATA, "액션유형이 LANDING인 경우 랜딩 종료시간(landingEndAt)은 필수로 입력되어야 합니다.");
            }
        }
    }
}