package com.winten.greenlight.admin.domain.action;

import com.winten.greenlight.admin.db.repository.mapper.action.ActionMapper;
import com.winten.greenlight.admin.domain.actionrule.ActionRuleService;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActionService {
    private final ActionMapper actionMapper;
    private final ActionRuleService actionRuleService;
    private final ActionCacheManager actionCacheManager;

    // TODO Action Rule 추가하기
    public List<Action> getAllActionsBySiteId() {
        return actionMapper.findAllAction();
    }

    public Action getActionById(Long actionId) {
        Action action = Action.builder()
                .id(actionId)
                .build();
        return actionMapper.findActionById(action)
                .orElseThrow(() -> CoreException.of(ErrorType.ACTION_NOT_FOUND, "액션을 찾을 수 없습니다. ID: " + actionId));
    }

    public Action getActionByIdWithRules(Long actionId) {
        Action action = getActionById(actionId);
        List<ActionRule> actionRules = actionRuleService.findAllActionRuleByActionId(actionId);
        action.setActionRules(actionRules);
        return action;
    }

    public List<Action> getActionsByGroup(Long actionGroupId) {
        Action param = Action.builder()
                .actionGroupId(actionGroupId)
                .build();
        List<Action> actions = actionMapper.findAllActionByGroupId(param);
        for (Action action : actions) {
            List<ActionRule> actionRules = actionRuleService.findAllActionRuleByActionId(action.getId());
            action.setActionRules(actionRules);
        }
        return actions;
    }

    @Transactional
    public Action createActionInGroup(
            Long actionGroupId,
            Action actionParam
    ) {
        // DB Insert
        actionParam.setActionGroupId(actionGroupId);
        actionParam.setLandingId(TSID.fast().toString()); // actionType과 관계없이 고유한 LandingId 부여

        validateActionType(actionParam); // actionType 검증

        // Action 저장
        Action actionResult = actionMapper.saveAction(actionParam);

        Long newActionId = actionResult.getId();

        // Action ID 세팅
        for (ActionRule actionRule : actionParam.getActionRules()) {
            actionRule.setActionId(newActionId);
        }
        // Action Rule 저장
        actionRuleService.saveAllActionRule(actionParam.getActionRules());
        List<ActionRule> actionRuleResult = actionRuleService.findAllActionRuleByActionId(newActionId);
        actionResult.setActionRules(actionRuleResult);

        Action actionUpdateResult = getActionById(newActionId);

        actionCacheManager.updateActionCache(actionUpdateResult);

        return actionUpdateResult;
    }

    public Action updateActionById(
            Action actionParam
    ) {
        var currentAction = getActionById(actionParam.getId()); // 존재여부 확인, 없으면 exception

        // 본인 Site가 아닐 경우 수정하면 안되므로 검증로직 추가 (SUPER 권한 제외)
        AuthUtil.ensureCanUpdate(currentAction.getSiteId());

        validateActionType(actionParam); // actionType 검증
        
        // DB Update
        actionMapper.updateActionById(actionParam);

        // TODO AWS처럼 action rule 개별 업데이트 및 삭제가 가능해야할수도?
        //  현재는 전체 삭제 후 다시 insert 중임
        // Action Rule 삭제 후 저장
        actionRuleService.deleteAllByActionId(actionParam.getId());

        for (ActionRule actionRule : actionParam.getActionRules()) {
            actionRule.setActionId(actionParam.getId());
        }
        actionRuleService.saveAllActionRule(actionParam.getActionRules());

        List<ActionRule> actionRuleResult = actionRuleService.findAllActionRuleByActionId(actionParam.getId());
        actionParam.setActionRules(actionRuleResult);

        Action actionUpdateResult = getActionById(actionParam.getId());

        actionCacheManager.updateActionCache(actionUpdateResult);

        return actionUpdateResult;
    }

    public Action deleteActionById(Long actionId) {
        Action action = getActionById(actionId); // 존재여부 확인, 없으면 exception

        // 본인 Site가 아닐 경우 수정하면 안되므로 검증로직 추가 (SUPER 권한 제외)
        AuthUtil.ensureCanDelete(action.getSiteId());

        // DB Delete
        actionMapper.deleteActionById(action);

        actionRuleService.deleteAllByActionId(actionId);

        actionCacheManager.deleteActionCache(action);

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

    public void reloadActionCache() {
        AuthUtil.ensureSuper();
        // 기존 액션 전체 삭제
        List<Action> allActions = getAllActionsBySiteId();
        for (Action action : allActions) {
            actionCacheManager.deleteActionCache(action);
        }

        List<Action> enabledActions = getAllActionsBySiteId();
        for (Action action : enabledActions) {
            List<ActionRule> actionRuleResult = actionRuleService.findAllActionRuleByActionId(action.getId());
            action.setActionRules(actionRuleResult);

            actionCacheManager.updateActionCache(action);
        }
    }
}