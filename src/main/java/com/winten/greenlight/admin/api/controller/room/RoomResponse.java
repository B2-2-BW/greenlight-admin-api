package com.winten.greenlight.admin.api.controller.room;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.winten.greenlight.admin.domain.action.DefaultRuleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private String roomId;
    private String name;
    private String description;
    private Integer maxTrafficPerSecond;
    private Integer capacity;
    private Boolean enabled;
    private DefaultRuleType defaultRuleType;
    private String defaultDestinationUrl;
    private String adImageUrl;
    private List<RoomRuleResponse> roomRules;
}