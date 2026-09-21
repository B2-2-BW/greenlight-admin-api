package com.winten.greenlight.admin.domain.alert;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AlertFingerprintTest {
    @Test
    void hashesSortedLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("room_id", "room-a");
        labels.put("alertname", "QUEUE_WAIT");
        labels.put("site_id", "site-a");
        labels.put("severity", "WARNING");

        Map<String, String> reversed = new LinkedHashMap<>();
        reversed.put("severity", "WARNING");
        reversed.put("site_id", "site-a");
        reversed.put("alertname", "QUEUE_WAIT");
        reversed.put("room_id", "room-a");

        assertThat(AlertFingerprint.of(labels)).isEqualTo(AlertFingerprint.of(reversed));
        assertThat(AlertFingerprint.of(labels)).hasSize(16);
    }
}
