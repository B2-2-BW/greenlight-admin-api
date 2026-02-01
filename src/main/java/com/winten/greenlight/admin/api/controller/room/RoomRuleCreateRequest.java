package com.winten.greenlight.admin.api.controller.room;

import com.winten.greenlight.admin.domain.action.MatchOperator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomRuleCreateRequest {
    private String value;
    private MatchOperator matchOperator;
    private String description;
}