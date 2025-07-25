package com.winten.greenlight.prototype.admin.db.repository.mapper.actiongroup;

import com.winten.greenlight.prototype.admin.domain.actiongroup.ActionGroup;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ActionGroupMapper {
    List<ActionGroup> findAll(ActionGroup actionGroup);
    List<ActionGroup> findAllEnabledWithActions(ActionGroup actionGroup);
    Optional<ActionGroup> findOneById(ActionGroup actionGroup);
    ActionGroup save(ActionGroup actionGroup);
    ActionGroup updateById(ActionGroup actionGroup);
    Long deleteById(ActionGroup actionGroup);
}