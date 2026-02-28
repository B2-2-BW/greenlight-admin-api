package com.winten.greenlight.admin.domain.dashboard;

import com.winten.greenlight.admin.domain.room.RoomMetric;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDetail {
    private Map<String, RoomMetric> detail;
    private String version;
    private int interval;

    public static DashboardDetail empty() {
        var d = new DashboardDetail();
        d.setDetail(new HashMap<>());
        return d;
    }
}