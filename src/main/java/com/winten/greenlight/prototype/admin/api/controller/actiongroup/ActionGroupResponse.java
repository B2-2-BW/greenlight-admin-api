package com.winten.greenlight.prototype.admin.api.controller.actiongroup;

import com.winten.greenlight.prototype.admin.domain.actiongroup.ActionGroup;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionGroupResponse {
    private Long id;
    private String ownerId;
    private String name;
    private String description;
    private Integer maxActiveCustomers;
    private Boolean enabled;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    public static ActionGroupResponse of(ActionGroup actionGroup) {
        return ActionGroupResponse.builder()
                .id(actionGroup.getId())
                .ownerId(actionGroup.getOwnerId())
                .name(actionGroup.getName())
                .description(actionGroup.getDescription())
                .maxActiveCustomers(actionGroup.getMaxActiveCustomers())
                .enabled(actionGroup.getEnabled())
                .createdBy(actionGroup.getCreatedBy())
                .createdAt(actionGroup.getCreatedAt())
                .updatedBy(actionGroup.getUpdatedBy())
                .updatedAt(actionGroup.getUpdatedAt())
                .build();
    }
}