package com.winten.greenlight.admin.domain.actionevent.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ActionEventTrafficSummary extends ActionEventTraffic {
    private long sessionCount;

    public static ActionEventTrafficSummary empty() {
        var empty = new ActionEventTrafficSummary();
        empty.setTimestamp(System.currentTimeMillis());
        return empty;
    }
}