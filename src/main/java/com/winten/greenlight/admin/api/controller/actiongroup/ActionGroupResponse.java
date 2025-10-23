package com.winten.greenlight.admin.api.controller.actiongroup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.winten.greenlight.admin.domain.action.Action;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionGroupResponse {
    private Long id;
    private String ownerId;
    private String name;
    private String description;
    private Integer maxTrafficPerSecond;
    private Boolean enabled;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Action> actions;
}