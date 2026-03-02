package com.winten.greenlight.admin.api.controller.dashboard;

import com.winten.greenlight.admin.domain.room.RoomMetric;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {
    private Map<String, RoomMetric> detail;
    private String version;
    private int interval;
}