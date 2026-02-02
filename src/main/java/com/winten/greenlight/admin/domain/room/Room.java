package com.winten.greenlight.admin.domain.room;

import com.winten.greenlight.admin.domain.action.DefaultRuleType;
import com.winten.greenlight.admin.support.dto.AuditDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Room extends AuditDto {
    private String roomId;
    private String siteId;
    private String name;
    private String description;
    private Integer maxTrafficPerSecond;
    private Integer capacity;
    private Boolean enabled;
    private DefaultRuleType defaultRuleType;
    private boolean updateRule;
    private List<RoomRule> roomRules;
}