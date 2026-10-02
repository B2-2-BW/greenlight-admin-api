package com.winten.greenlight.admin.domain.alert;

import java.util.Locale;

public enum AlertStatus {
    FIRING,
    RESOLVED;

    public static AlertStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("alert status is required");
        }
        return valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
