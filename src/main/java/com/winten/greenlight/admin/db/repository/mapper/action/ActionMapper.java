package com.winten.greenlight.admin.db.repository.mapper.action;

import com.winten.greenlight.admin.domain.action.Action;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ActionMapper {
    // TODO 통신 시 ActionEntity로 왔다갔다 하도록 개선해야함
    List<Action> findAllAction();
    List<Action> findAllEnabledAction(String ownerId);
    List<Action> findAllActionByGroupId(Action action);
    Optional<Action> findActionById(Action action);
    Action saveAction(Action action);
    Action updateActionById(Action action);
    Long deleteActionById(Action action);
}