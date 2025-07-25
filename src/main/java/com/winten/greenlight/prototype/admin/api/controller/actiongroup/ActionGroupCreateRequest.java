package com.winten.greenlight.prototype.admin.api.controller.actiongroup;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionGroupCreateRequest {
    @NotNull
    private String name;
    private String description;
    @NotNull
    private Integer maxActiveCustomers;
    @NotNull
    private Boolean enabled;
}