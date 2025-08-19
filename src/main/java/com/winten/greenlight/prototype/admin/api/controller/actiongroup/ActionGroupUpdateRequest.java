package com.winten.greenlight.prototype.admin.api.controller.actiongroup;

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
    private Integer maxTrafficPerSecond;
    private Boolean enabled;
}