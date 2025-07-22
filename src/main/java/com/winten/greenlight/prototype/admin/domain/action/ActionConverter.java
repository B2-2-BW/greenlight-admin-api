package com.winten.greenlight.prototype.admin.domain.action;

import com.winten.greenlight.prototype.admin.api.controller.action.ActionCreateRequest;
import com.winten.greenlight.prototype.admin.api.controller.action.ActionUpdateRequest;
import com.winten.greenlight.prototype.admin.api.controller.action.ActionResponse;
import com.winten.greenlight.prototype.admin.db.repository.mapper.action.ActionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ActionConverter {
    ActionEntity toEntity(Action action);
    Action toDto(ActionCreateRequest actionCreateRequest);
    Action toDto(ActionUpdateRequest actionUpdateRequest);
    ActionResponse toResponse(Action action);
}