package com.winten.greenlight.admin.api.controller.alert;

import com.winten.greenlight.admin.domain.alert.AlertPolicy;
import com.winten.greenlight.admin.domain.alert.AlertPolicyService;
import com.winten.greenlight.admin.domain.alert.QueueAlertCompare;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AlertPolicyController {
    private final AlertPolicyService alertPolicyService;

    @GetMapping("/sites/{siteId}/alert-policy")
    public ResponseEntity<AlertPolicy> get(@PathVariable String siteId) {
        return ResponseEntity.ok(alertPolicyService.get(siteId));
    }

    @PutMapping("/sites/{siteId}/alert-policy")
    public ResponseEntity<AlertPolicy> update(
            @PathVariable String siteId,
            @RequestBody @Valid AlertPolicyUpdateRequest request
    ) {
        return ResponseEntity.ok(alertPolicyService.update(siteId, request.toPolicy()));
    }

    @PostMapping("/sites/{siteId}/alert-policy/cache")
    public ResponseEntity<String> reloadCache(@PathVariable String siteId) {
        alertPolicyService.reloadCache(siteId);
        return ResponseEntity.ok("alert policy cache reload successful");
    }

    @PostMapping("/alert-policy/cache")
    public ResponseEntity<String> reloadAllCache() {
        int count = alertPolicyService.reloadAllCache();
        return ResponseEntity.ok("alert policy cache reload successful: " + count);
    }

    @Data
    public static class AlertPolicyUpdateRequest {
        @Min(1)
        private int forTicks;
        @Min(1)
        private int repeatIntervalSeconds;
        @NotNull
        private QueueAlertCompare waitingCompare;
        @NotNull
        @DecimalMin("0.0001")
        private Double waitingThreshold;

        AlertPolicy toPolicy() {
            return AlertPolicy.builder()
                    .forTicks(forTicks)
                    .repeatIntervalSeconds(repeatIntervalSeconds)
                    .waitingCompare(waitingCompare)
                    .waitingThreshold(waitingThreshold)
                    .build();
        }
    }
}
