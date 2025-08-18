package com.winten.greenlight.prototype.admin.domain.actionevent;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActionEventTrafficSummary extends ActionEventTraffic {
    private long sessionCount;

    public static ActionEventTrafficSummary empty() {
        var empty = new ActionEventTrafficSummary();
        empty.setTimestamp(System.currentTimeMillis());
        return empty;
    }
}