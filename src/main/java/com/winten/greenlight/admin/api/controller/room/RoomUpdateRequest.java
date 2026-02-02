package com.winten.greenlight.admin.api.controller.room;

import com.winten.greenlight.admin.domain.action.DefaultRuleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomUpdateRequest {
    private String name;
    private String description;
    private Integer maxTrafficPerSecond;
    private Integer capacity;
    private Boolean enabled;
    private DefaultRuleType defaultRuleType;
    private boolean updateRule;
    private List<RoomRuleCreateRequest> roomRules;
}