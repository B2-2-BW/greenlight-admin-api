package com.winten.greenlight.admin.api.controller.alert;

import com.winten.greenlight.admin.domain.alert.AlertSilence;
import com.winten.greenlight.admin.domain.alert.AlertSilenceService;
import com.winten.greenlight.admin.domain.alert.AlertSilenceSystem;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
public class AlertSilenceController {
    private final AlertSilenceService alertSilenceService;

    @GetMapping
    public ResponseEntity<AlertSilenceListResponse> list(
            @RequestParam(required = false) String systemCode,
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        return ResponseEntity.ok(new AlertSilenceListResponse(
                alertSilenceService.list(systemCode, activeOnly),
                Arrays.stream(AlertSilenceSystem.values())
                        .map(system -> new AlertSilenceSystemOption(
                                system.name(),
                                system.label(),
                                system.alertnames()
                        ))
                        .toList()
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

    @PostMapping("/{silenceId}/cancel")
    public ResponseEntity<AlertSilence> cancel(@PathVariable long silenceId) {
        return ResponseEntity.ok(alertSilenceService.cancel(silenceId));
    }

    public record AlertSilenceListResponse(
            List<AlertSilence> silences,
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
}
