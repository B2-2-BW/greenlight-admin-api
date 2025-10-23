package com.winten.greenlight.admin.db.repository.mapper.action;

import com.winten.greenlight.admin.domain.action.ActionRule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ActionRuleMapper {
    List<ActionRule> findAllByActionId(Long actionId);
    ActionRule save(ActionRule actionRule);
    ActionRule updateById(ActionRule actionRule);
    Long deleteAllByActionId(Long id);
    Long deleteById(Long id);
}