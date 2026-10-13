package com.winten.greenlight.admin.domain.alert;

import java.util.List;
import java.util.Locale;

public enum AlertSilenceSystem {
    QUEUE("대기열", List.of("QUEUE_WAIT")),
    SCHEDULER("스케줄러", List.of("SCHEDULER_FAILED", "SCHEDULER_STOPPED")),
    DOCKER("Docker 컨테이너", List.of("ContainerDown")),
    REDIS("Redis", List.of("RedisClusterDown")),
    HOST("호스트", List.of("HostHighCpuLoad", "HostHighMemoryUsage", "HostLowDiskSpace")),
    TARGET("Prometheus 수집", List.of("PrometheusTargetDown"));

    private final String label;
    private final List<String> alertnames;

    AlertSilenceSystem(String label, List<String> alertnames) {
        this.label = label;
        this.alertnames = List.copyOf(alertnames);
    }

    public String label() {
        return label;
    }

    public List<String> alertnames() {
        return alertnames;
    }

    public static AlertSilenceSystem fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static AlertSilenceSystem ofAlertname(String alertname) {
        if (alertname == null || alertname.isBlank()) {
            return null;
        }
        String name = alertname.trim();
        for (AlertSilenceSystem system : values()) {
            if (system.alertnames.contains(name)) {
                return system;
            }
        }
        return null;
    }
}
