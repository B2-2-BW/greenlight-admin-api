package com.winten.greenlight.prototype.admin.api.controller.actiongroup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionGroupCreateRequest {
    private String name;
    private String description;
    private Integer maxActiveCustomers;
    private boolean enabled;
}