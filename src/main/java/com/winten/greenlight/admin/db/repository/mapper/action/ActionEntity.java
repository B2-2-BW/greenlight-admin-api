package com.winten.greenlight.admin.db.repository.mapper.action;

import com.winten.greenlight.admin.domain.action.ActionRule;
import com.winten.greenlight.admin.domain.action.ActionType;
import com.winten.greenlight.admin.domain.action.DefaultRuleType;
import com.winten.greenlight.admin.support.dto.Hashable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionEntity implements Hashable {
    private Long id;
    private Long actionGroupId;
    private String ownerId;
    private String name;
    private String actionUrl;
    private ActionType actionType;
    private String landingId;
    private LocalDateTime landingStartAt;
    private LocalDateTime landingEndAt;
    private String landingDestinationUrl;
    private DefaultRuleType defaultRuleType;
    private List<ActionRule> actionRules;
}