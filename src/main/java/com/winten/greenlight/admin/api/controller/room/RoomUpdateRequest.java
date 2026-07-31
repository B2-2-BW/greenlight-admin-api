package com.winten.greenlight.admin.api.controller.room;

import com.winten.greenlight.admin.domain.action.DefaultRuleType;
import com.winten.greenlight.admin.domain.room.RoomEnvironment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Size;

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
    private String defaultDestinationUrl;
    private RoomEnvironment roomEnvironment;
    private boolean updateRule;
    private String adImageUrl;
    private List<RoomRuleCreateRequest> roomRules;
    @Size(max = 1000)
    private String reason;
}
