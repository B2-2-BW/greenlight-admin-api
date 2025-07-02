package com.winten.greenlight.prototype.admin.api.controller.actiongroup;

import com.winten.greenlight.prototype.admin.domain.actiongroup.ActionGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionGroupUpdateRequest {
    private String name;
    private String ownerId;
    private String description;
    private Integer maxActiveCustomers;
    private boolean enabled;
    public ActionGroup toActionGroup() {
        return ActionGroup.builder()
                .name(name)
                .ownerId(ownerId)
                .description(description)
                .maxActiveCustomers(maxActiveCustomers)
                .enabled(enabled)
                .build();
    }
}