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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String name;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer maxTrafficPerSecond;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer capacity;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean enabled;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DefaultRuleType defaultRuleType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<RoomRuleResponse> roomRules;
}