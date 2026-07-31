package com.winten.greenlight.admin.api.controller.systemstatus;

import com.winten.greenlight.admin.domain.systemstatus.SystemStatusService;
import com.winten.greenlight.admin.support.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system-status")
@RequiredArgsConstructor
public class SystemStatusController {
    private final SystemStatusService systemStatusService;

    @GetMapping
    public ResponseEntity<SystemStatusResponse> getSystemStatus() {
        AuthUtil.ensureSuper();
        return ResponseEntity.ok(systemStatusService.getSystemStatus());
    }
}
