package com.winten.greenlight.admin.api.controller.room;

import com.winten.greenlight.admin.domain.action.DefaultRuleType;
import com.winten.greenlight.admin.domain.room.RoomEnvironment;
import jakarta.validation.constraints.NotNull;
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
    private RoomEnvironment roomEnvironment;
    private Integer maxTrafficPerSecond;
    private Integer capacity;
    @NotNull
    private Boolean enabled;
    private String defaultDestinationUrl;
    private DefaultRuleType defaultRuleType;
    private String adImageUrl;
    private List<RoomRuleCreateRequest> roomRules;
}
