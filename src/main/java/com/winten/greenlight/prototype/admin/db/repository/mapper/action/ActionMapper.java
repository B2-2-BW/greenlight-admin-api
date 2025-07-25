package com.winten.greenlight.prototype.admin.db.repository.mapper.action;

import com.winten.greenlight.prototype.admin.domain.action.Action;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ActionMapper {
    // TODO 통신 시 ActionEntity로 왔다갔다 하도록 개선해야함
    List<Action> findAll(String ownerId);
    List<Action> findAllByGroupId(Action action);
    Optional<Action> findOneById(Action action);
    Action save(Action action);
    Action updateById(Action action);
    Long deleteById(Action action);
}