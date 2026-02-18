package com.winten.greenlight.admin.api.controller.dashboard;

import com.winten.greenlight.admin.domain.dashboard.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {
    private Map<String, RoomStatus> detail;
}

