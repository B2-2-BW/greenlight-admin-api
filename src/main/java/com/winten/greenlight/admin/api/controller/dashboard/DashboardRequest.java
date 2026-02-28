package com.winten.greenlight.admin.api.controller.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;

@Data
@AllArgsConstructor
public class DashboardRequest {
    private String version;
}