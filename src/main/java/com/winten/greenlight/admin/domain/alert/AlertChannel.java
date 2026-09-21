package com.winten.greenlight.admin.domain.alert;

import java.util.List;
import java.util.Locale;

public enum AlertChannel {
    TEAMS("Teams"),
    KAKAO("카카오 알림톡"),
    EMAIL("이메일");

    private final String label;

    AlertChannel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static boolean isKnown(String channel) {
        if (channel == null || channel.isBlank()) {
            return false;
        }
        try {
            valueOf(channel.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static List<AlertChannel> catalog() {
        return List.of(values());
    }
}
