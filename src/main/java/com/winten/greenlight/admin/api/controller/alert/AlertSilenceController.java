package com.winten.greenlight.admin.api.controller.alert;

import com.winten.greenlight.admin.domain.alert.AlertSilence;
import com.winten.greenlight.admin.domain.alert.AlertSilencePage;
import com.winten.greenlight.admin.domain.alert.AlertSilenceService;
import com.winten.greenlight.admin.domain.alert.AlertSilenceSystem;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/alert-silences")
@RequiredArgsConstructor
@Validated
public class AlertSilenceController {
    private final AlertSilenceService alertSilenceService;

    @GetMapping
    public ResponseEntity<AlertSilenceListResponse> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        String effectiveStatus = status == null || status.isBlank()
                ? (activeOnly ? "ACTIVE" : null)
                : status;
        AlertSilencePage result = alertSilenceService.list(systemCode, effectiveStatus, page, size);
        return ResponseEntity.ok(new AlertSilenceListResponse(
                result.getContent(),
                result.getPage(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                systems()
        ));
    }

    @PostMapping
    public ResponseEntity<AlertSilence> create(@RequestBody AlertSilenceCreateRequest request) {
        AlertSilenceCreateRequest body = request == null ? new AlertSilenceCreateRequest() : request;
        return ResponseEntity.status(HttpStatus.CREATED).body(alertSilenceService.create(
                body.getSystemCode(),
                body.getStartsAt(),
                body.getEndsAt(),
                body.getComment()
        ));
    }

    @PutMapping("/{silenceId}")
    public ResponseEntity<AlertSilence> update(
            @PathVariable long silenceId,
            @RequestBody AlertSilenceUpdateRequest request
    ) {
        AlertSilenceUpdateRequest body = request == null ? new AlertSilenceUpdateRequest() : request;
        return ResponseEntity.ok(alertSilenceService.update(
                silenceId,
                body.getStartsAt(),
                body.getEndsAt(),
                body.getComment()
        ));
    }

    @PostMapping("/{silenceId}/cancel")
    public ResponseEntity<AlertSilence> cancel(@PathVariable long silenceId) {
        return ResponseEntity.ok(alertSilenceService.cancel(silenceId));
    }

    private static List<AlertSilenceSystemOption> systems() {
        return Arrays.stream(AlertSilenceSystem.values())
                .map(system -> new AlertSilenceSystemOption(system.name(), system.label(), system.alertnames()))
                .toList();
    }

    public record AlertSilenceListResponse(
            List<AlertSilence> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            List<AlertSilenceSystemOption> systems
    ) {
    }

    public record AlertSilenceSystemOption(String systemCode, String label, List<String> alertnames) {
    }

    @Data
    public static class AlertSilenceCreateRequest {
        private String systemCode;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
        private String comment;
    }

    @Data
    public static class AlertSilenceUpdateRequest {
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
        private String comment;
    }
}
