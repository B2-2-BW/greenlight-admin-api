package com.winten.greenlight.admin.api.controller.dashboard;

import com.winten.greenlight.admin.domain.dashboard.DashboardConverter;
import com.winten.greenlight.admin.domain.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;
    private final DashboardConverter dashboardConverter;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboardDetail() {
        var detail = dashboardService.getDashboardDetail();
        return ResponseEntity.ok(dashboardConverter.toResponse(detail));
    }
}
