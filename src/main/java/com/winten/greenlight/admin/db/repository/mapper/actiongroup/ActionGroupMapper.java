package com.winten.greenlight.admin.db.repository.mapper.actiongroup;

import com.winten.greenlight.admin.domain.actiongroup.ActionGroup;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ActionGroupMapper {
    List<ActionGroup> findAllActionGroup();
    List<ActionGroup> findAllEnabledWithActions();
    Optional<ActionGroup> findOneById(ActionGroup actionGroup);
    ActionGroup saveActionGroup(ActionGroup actionGroup);
    ActionGroup updateActionGroupById(ActionGroup actionGroup);
    Long deleteActionGroupById(ActionGroup actionGroup);
}