package com.winten.greenlight.admin.db.repository.mapper.action;

import com.winten.greenlight.admin.domain.action.ActionRule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ActionRuleMapper {
    List<ActionRule> findAllActionRuleByActionId(ActionRule actionRule);
    ActionRule saveActionRule(ActionRule actionRule);
    ActionRule updateActionRuleById(ActionRule actionRule);
    Long deleteAllActionRuleByActionId(Long id);
    Long deleteActionRuleById(Long id);
}