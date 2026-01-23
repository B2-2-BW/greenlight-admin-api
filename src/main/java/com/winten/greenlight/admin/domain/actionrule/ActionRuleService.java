package com.winten.greenlight.admin.domain.actionrule;

import com.winten.greenlight.admin.db.repository.mapper.action.ActionRuleMapper;
import com.winten.greenlight.admin.domain.action.ActionRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActionRuleService {
    private final ActionRuleMapper actionRuleMapper;

    public List<ActionRule> findAllActionRuleByActionId(Long actionId) {
        List<ActionRule> actionRules = actionRuleMapper.findAllActionRuleByActionId(actionId);
        if (actionRules == null) {
            actionRules = List.of();
        }
        return actionRules;
    }

    public void saveActionRule(ActionRule actionRule) {
        actionRuleMapper.saveActionRule(actionRule);
    }

    public void saveAllActionRule(List<ActionRule> actionRules) {
        for (ActionRule actionRule : actionRules) {
            saveActionRule(actionRule);
        }
    }

    public void deleteAllByActionId(Long actionId) {
        actionRuleMapper.deleteAllActionRuleByActionId(actionId);
    }
}