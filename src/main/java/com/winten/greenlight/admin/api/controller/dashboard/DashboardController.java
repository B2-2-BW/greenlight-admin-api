package com.winten.greenlight.admin.api.controller.dashboard;

import com.winten.greenlight.admin.domain.dashboard.DashboardConverter;
import com.winten.greenlight.admin.domain.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;
    private final DashboardConverter dashboardConverter;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboardDetail(
            @ParameterObject DashboardRequest request
    ) {
        var detail = dashboardService.getDashboardDetail(request);
        return ResponseEntity.ok(dashboardConverter.toResponse(detail));
    }
}