package com.winten.greenlight.prototype.admin.api.controller.action;

import com.winten.greenlight.prototype.admin.domain.action.Action;
import com.winten.greenlight.prototype.admin.domain.action.ActionRule;
import com.winten.greenlight.prototype.admin.domain.action.ActionType;
import com.winten.greenlight.prototype.admin.domain.action.DefaultRuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public Action toAction() {
        return Action.builder()
                .name(name)
                .actionUrl(actionUrl)
                .actionType(actionType)
                .landingStartAt(landingStartAt)
                .landingEndAt(landingEndAt)
                .landingDestinationUrl(landingDestinationUrl)
                .defaultRuleType(defaultRuleType)
                .build();
    }

    public List<ActionRule> toActionRules() {
        return actionRules != null ? actionRules : new ArrayList<>();
    }
}