package com.winten.greenlight.prototype.admin.domain.actiongroup;

import com.winten.greenlight.prototype.admin.api.controller.actiongroup.ActionGroupCreateRequest;
import com.winten.greenlight.prototype.admin.api.controller.actiongroup.ActionGroupResponse;
import com.winten.greenlight.prototype.admin.api.controller.actiongroup.ActionGroupUpdateRequest;
import com.winten.greenlight.prototype.admin.db.repository.mapper.actiongroup.ActionGroupEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ActionGroupConverter {
    ActionGroupEntity toEntity(ActionGroup actionGroup);
    ActionGroup toDto(ActionGroupCreateRequest actionGroupCreateRequest);
    ActionGroup toDto(ActionGroupUpdateRequest actionGroupUpdateRequest);
    ActionGroupResponse toResponse(ActionGroup actionGroupEntity);
}