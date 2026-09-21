package com.winten.greenlight.admin.domain.alert;

import java.util.List;
import java.util.Locale;

public enum AlertCatalog {
    QUEUE_WAIT("대기인원"),
    SITE_DISABLED("사이트 비활성화"),
    SITE_MAINTENANCE("사이트 점검"),
    INFRA("인프라");

    private final String label;

    AlertCatalog(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static boolean isKnown(String alertname) {
        if (alertname == null || alertname.isBlank()) {
            return false;
        }
        try {
            valueOf(alertname.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static String subscriptionKey(String alertname) {
        if (alertname == null || alertname.isBlank()) {
            return INFRA.name();
        }
        String normalized = alertname.trim();
        return isKnown(normalized) ? valueOf(normalized.toUpperCase(Locale.ROOT)).name() : INFRA.name();
    }

    public boolean isPlatformWide() {
        return this == INFRA;
    }

    public static List<AlertCatalog> catalog() {
        return List.of(values());
    }

    public static List<AlertCatalog> catalog(boolean includePlatformWide) {
        if (includePlatformWide) {
            return catalog();
        }
        return List.of(QUEUE_WAIT, SITE_DISABLED, SITE_MAINTENANCE);
    }
}
