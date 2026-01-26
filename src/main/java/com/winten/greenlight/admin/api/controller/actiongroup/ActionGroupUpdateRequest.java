package com.winten.greenlight.admin.api.controller.actiongroup;

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
    private String description;
    private Integer maxTrafficPerSecond;
    private Integer roomCapacity;
    private Boolean enabled;
}