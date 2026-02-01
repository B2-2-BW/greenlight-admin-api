package com.winten.greenlight.admin.api.controller.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreateRequest {
    private String name;
    private String description;
    private Integer maxTrafficPerSecond;
    private Integer capacity;
    private boolean enabled;
    private String defaultRuleType;
    private List<RoomRuleCreateRequest> roomRules;
}