package com.winten.greenlight.admin.api.controller.dashboard;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DashboardRequest {
    @Parameter
    private String version;
    @Parameter
    private List<String> roomIdList;
}