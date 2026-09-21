package com.winten.greenlight.admin.api.controller.alert;

import com.winten.greenlight.admin.domain.alert.AlertLogPage;
import com.winten.greenlight.admin.domain.alert.AlertService;
import com.winten.greenlight.admin.domain.alert.AlertStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/alert-logs")
@RequiredArgsConstructor
@Validated
public class AlertLogController {
    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<AlertLogPage> getAlertLogs(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String alertname,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(alertService.getAlertLogs(
                page, size, alertname, status, from, to
        ));
    }
}
