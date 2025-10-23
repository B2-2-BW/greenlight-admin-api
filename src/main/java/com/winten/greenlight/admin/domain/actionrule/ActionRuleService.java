package com.winten.greenlight.admin.domain.actionrule;

import com.winten.greenlight.admin.db.repository.mapper.action.ActionRuleMapper;
import com.winten.greenlight.admin.domain.action.ActionRule;
import com.winten.greenlight.admin.domain.user.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActionRuleService {
    private final ActionRuleMapper actionRuleMapper;

    public List<ActionRule> findAllActionRuleByActionId(Long actionId) {
        List<ActionRule> actionRules = actionRuleMapper.findAllByActionId(actionId);
        if (actionRules == null) {
            actionRules = List.of();
        }
        return actionRules;
    }

    public void save(ActionRule actionRule, CurrentUser currentUser) {
        actionRule.setCreatedBy(currentUser.getUserId());
        actionRule.setUpdatedBy(currentUser.getUserId());
        actionRuleMapper.save(actionRule);
    }

    public void saveAll(List<ActionRule> actionRules, CurrentUser currentUser) {
        for (ActionRule actionRule : actionRules) {
            save(actionRule, currentUser);
        }
    }

    public void deleteAllByActionId(Long actionId) {
        actionRuleMapper.deleteAllByActionId(actionId);
    }
}