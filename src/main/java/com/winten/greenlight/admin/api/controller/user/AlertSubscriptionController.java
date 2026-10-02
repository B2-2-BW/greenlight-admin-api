package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.alert.AlertSubscription;
import com.winten.greenlight.admin.domain.alert.AlertSubscriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/me/alert-subscriptions")
@RequiredArgsConstructor
public class AlertSubscriptionController {
    private final AlertSubscriptionService alertSubscriptionService;

    @GetMapping
    public ResponseEntity<AlertSubscriptionListResponse> getMine() {
        return ResponseEntity.ok(new AlertSubscriptionListResponse(alertSubscriptionService.getMine()));
    }

    @PutMapping
    public ResponseEntity<AlertSubscriptionListResponse> updateMine(
            @RequestBody @Valid AlertSubscriptionUpdateRequest request
    ) {
        return ResponseEntity.ok(new AlertSubscriptionListResponse(
                alertSubscriptionService.updateMine(request.getSubscriptions())
        ));
    }

    public record AlertSubscriptionListResponse(List<AlertSubscription> subscriptions) {
    }

    @Data
    public static class AlertSubscriptionUpdateRequest {
        @NotEmpty
        private List<AlertSubscription> subscriptions;
    }
}
