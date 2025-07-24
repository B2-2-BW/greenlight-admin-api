package com.winten.greenlight.prototype.admin.api.controller.action;

import com.winten.greenlight.prototype.admin.domain.action.ActionRule;
import com.winten.greenlight.prototype.admin.domain.action.ActionType;
import com.winten.greenlight.prototype.admin.domain.action.DefaultRuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionCreateRequest {
    private String name;
    private String actionUrl;
    private ActionType actionType;
    private LocalDateTime landingStartAt;
    private LocalDateTime landingEndAt;
    private String landingDestinationUrl;
    private DefaultRuleType defaultRuleType;
    private List<ActionRule> actionRules;
}