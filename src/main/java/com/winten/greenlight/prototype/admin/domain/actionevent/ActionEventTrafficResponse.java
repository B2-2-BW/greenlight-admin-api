package com.winten.greenlight.prototype.admin.domain.actionevent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActionEventTrafficResponse {
    private ActionEventTraffic summary;
    private Map<Long, ActionEventTraffic> detail;
}