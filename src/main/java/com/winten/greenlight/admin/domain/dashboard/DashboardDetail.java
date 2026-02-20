package com.winten.greenlight.admin.domain.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDetail {
    private Map<String, RoomStatus> detail;
    private long timestamp;

    public static DashboardDetail empty() {
        var d = new DashboardDetail();
        d.setDetail(new HashMap<>());
        d.setTimestamp(System.currentTimeMillis());
        return d;
    }
}