package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.alert.AlertChannel;
import com.winten.greenlight.admin.domain.alert.AlertChannelService;
import com.winten.greenlight.admin.domain.alert.AlertChannelTarget;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/users/me/alert-channels")
@RequiredArgsConstructor
public class AlertChannelController {
    private final AlertChannelService alertChannelService;

    @GetMapping
    public ResponseEntity<AlertChannelListResponse> getMine() {
        return ResponseEntity.ok(new AlertChannelListResponse(
                alertChannelService.getMine(),
                Arrays.stream(AlertChannel.values())
                        .map(item -> new AlertChannelOption(item.name(), item.label()))
                        .toList()
        ));
    }

    @PutMapping
    public ResponseEntity<AlertChannelListResponse> replaceMine(
            @RequestBody @Valid AlertChannelUpdateRequest request
    ) {
        return ResponseEntity.ok(new AlertChannelListResponse(
                alertChannelService.replaceMine(request.getTargets() == null ? List.of() : request.getTargets()),
                Arrays.stream(AlertChannel.values())
                        .map(item -> new AlertChannelOption(item.name(), item.label()))
                        .toList()
        ));
    }

    public record AlertChannelListResponse(
            List<AlertChannelTarget> targets,
            List<AlertChannelOption> catalog
    ) {
    }

    public record AlertChannelOption(String channel, String label) {
    }

    @Data
    public static class AlertChannelUpdateRequest {
        private List<AlertChannelTarget> targets;
    }
}
