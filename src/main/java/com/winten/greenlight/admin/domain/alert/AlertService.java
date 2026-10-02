package com.winten.greenlight.admin.domain.alert;

import com.winten.greenlight.admin.api.controller.webhook.AlertManagerRequest;
import com.winten.greenlight.admin.db.repository.mapper.alert.AlertLogMapper;
import com.winten.greenlight.admin.db.repository.mapper.alert.AlertSendLogMapper;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import com.winten.greenlight.admin.support.util.RequestScopeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {
    static final Duration MAX_RANGE = Duration.ofDays(7);
    static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZONE_ID);
    private final AlertLogMapper alertLogMapper;
    private final AlertSendLogMapper alertSendLogMapper;
    private final TeamsAlertClient teamsAlertClient;
    private final AlertSubscriptionService alertSubscriptionService;
    private final JsonMapper jsonMapper;
    private final Environment environment;

    @Transactional
    public void apply(List<AlertManagerRequest.Alert> alerts, String createdBy) {
        if (alerts == null || alerts.isEmpty()) {
            return;
        }
        String actor = normalizeActor(createdBy);
        String ip = requestIp();
        LocalDateTime now = LocalDateTime.now();
        List<AlertManagerRequest.Alert> accepted = new ArrayList<>();
        for (AlertManagerRequest.Alert alert : alerts) {
            if (applyOne(alert, actor, ip, now)) {
                accepted.add(alert);
            }
        }
        scheduleSend(accepted, actor, ip);
    }

    @Transactional
    public void applySiteStatusAlert(String siteId, AlertCatalog catalog, boolean firing, String summary, String description) {
        AlertStatus status = firing ? AlertStatus.FIRING : AlertStatus.RESOLVED;
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("alertname", catalog.name());
        labels.put("site_id", siteId);
        labels.put("severity", AlertSeverity.WARNING.name());
        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put("summary", summary);
        if (description != null && !description.isBlank()) {
            annotations.put("description", description);
        }
        String occurredAt = Instant.now().toString();
        annotations.put("occurred_at", occurredAt);
        AlertManagerRequest.Alert alert = new AlertManagerRequest.Alert();
        alert.setStatus(status.name());
        alert.setLabels(labels);
        alert.setAnnotations(annotations);
        alert.setFingerprint(AlertFingerprint.of(labels));
        if (firing) {
            alert.setStartsAt(occurredAt);
        } else {
            alert.setEndsAt(occurredAt);
        }
        String createdBy = AuthUtil.getCurrentUser().getUserId();
        apply(List.of(alert), createdBy == null ? "admin" : createdBy);
    }

    @Transactional
    public void applyPlatformAlert(
            String alertname,
            String schedulerCode,
            boolean firing,
            String summary,
            String description,
            String createdBy
    ) {
        AlertStatus status = firing ? AlertStatus.FIRING : AlertStatus.RESOLVED;
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("alertname", alertname);
        labels.put("scheduler_code", schedulerCode);
        labels.put("severity", AlertSeverity.CRITICAL.name());
        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put("summary", summary);
        if (description != null && !description.isBlank()) {
            annotations.put("description", description);
        }
        String occurredAt = Instant.now().toString();
        annotations.put("occurred_at", occurredAt);
        AlertManagerRequest.Alert alert = new AlertManagerRequest.Alert();
        alert.setStatus(status.name());
        alert.setLabels(labels);
        alert.setAnnotations(annotations);
        alert.setFingerprint(AlertFingerprint.of(labels));
        if (firing) {
            alert.setStartsAt(occurredAt);
        } else {
            alert.setEndsAt(occurredAt);
        }
        apply(List.of(alert), createdBy);
    }

    @Transactional(readOnly = true)
    public AlertLogPage getAlertLogs(
            int requestedPage,
            int size,
            String alertname,
            AlertStatus status,
            LocalDateTime from,
            LocalDateTime to
    ) {
        AuthUtil.ensureUserAdmin();
        validateRange(from, to);
        String siteId = AuthUtil.getCurrentUser().getUserSiteId();
        String normalizedAlertname = normalize(alertname);
        String normalizedStatus = status == null ? null : status.name();
        long totalElements = alertLogMapper.count(siteId, normalizedAlertname, normalizedStatus, from, to);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int page = totalPages == 0 ? 1 : Math.min(requestedPage, totalPages);
        List<AlertLog> content = totalElements == 0
                ? List.of()
                : alertLogMapper.findPage(
                        siteId, normalizedAlertname, normalizedStatus, from, to,
                        size, (long) (page - 1) * size
                );
        return new AlertLogPage(content, page, size, totalElements, totalPages);
    }

    private boolean applyOne(AlertManagerRequest.Alert alert, String actor, String ip, LocalDateTime now) {
        if (alert == null || alert.getStatus() == null) {
            return false;
        }
        AlertStatus status;
        try {
            status = AlertStatus.from(alert.getStatus());
        } catch (IllegalArgumentException ignored) {
            throw CoreException.of(ErrorType.INVALID_DATA, "alert status는 FIRING 또는 RESOLVED 여야 합니다.");
        }
        Map<String, String> labels = alert.getLabels() == null ? Map.of() : alert.getLabels();
        String alertname = firstNonBlank(labels.get("alertname"), "Unknown");
        String fingerprint = firstNonBlank(alert.getFingerprint(), AlertFingerprint.of(ensureAlertname(labels, alertname)));
        LocalDateTime startedAt = parseTime(alert.getStartsAt(), now);
        LocalDateTime endedAt = parseTime(alert.getEndsAt(), now);
        Map<String, String> annotations = new LinkedHashMap<>();
        if (alert.getAnnotations() != null) {
            annotations.putAll(alert.getAnnotations());
        }
        if (!annotations.containsKey("occurred_at") || annotations.get("occurred_at") == null || annotations.get("occurred_at").isBlank()) {
            annotations.put("occurred_at", firstNonBlank(alert.getStartsAt(), alert.getEndsAt(), Instant.now().toString()));
        }
        alert.setAnnotations(annotations);

        AlertLog row = AlertLog.builder()
                .fingerprint(fingerprint)
                .alertname(alertname)
                .status(status)
                .siteId(blankToNull(labels.get("site_id")))
                .roomId(blankToNull(labels.get("room_id")))
                .labels(writeJson(labels))
                .annotations(writeJson(annotations))
                .startedAt(startedAt)
                .endedAt(status == AlertStatus.RESOLVED ? endedAt : null)
                .createdBy(actor)
                .createdAt(now)
                .createdIp(ip)
                .updatedBy(actor)
                .updatedAt(now)
                .updatedIp(ip)
                .build();

        if (status == AlertStatus.FIRING) {
            alertLogMapper.upsertFiring(row);
            return true;
        }
        int updated = alertLogMapper.resolve(row);
        if (updated == 0) {
            log.info("Skip resolved alert with no firing row. fingerprint={} alertname={}", fingerprint, alertname);
            return false;
        }
        return true;
    }

    private void scheduleSend(List<AlertManagerRequest.Alert> alerts, String actor, String ip) {
        if (alerts.isEmpty()) {
            return;
        }
        Runnable send = () -> {
            try {
                sendTeams(alerts, actor, ip);
            } catch (Exception exception) {
                log.error("Alert send failed after persist. size={}", alerts.size(), exception);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(send);
                }
            });
        } else {
            CompletableFuture.runAsync(send);
        }
    }

    private void sendTeams(List<AlertManagerRequest.Alert> alerts, String actor, String ip) {
        for (AlertManagerRequest.Alert alert : alerts) {
            String alertname = alert.getLabels() == null ? null : alert.getLabels().get("alertname");
            String siteId = alert.getLabels() == null ? null : alert.getLabels().get("site_id");
            List<String> targets = alertSubscriptionService.targetsFor(alertname, siteId, AlertChannel.TEAMS);
            if (targets.isEmpty()) {
                log.info("Teams alert skipped: no subscribers. alertname={}", alertname);
                continue;
            }
            String sentAt = Instant.now().toString();
            String summary = null;
            String description = null;
            String occurredAt = null;
            if (alert.getAnnotations() != null) {
                summary = alert.getAnnotations().get("summary");
                description = alert.getAnnotations().get("description");
                occurredAt = alert.getAnnotations().get("occurred_at");
            }
            if (occurredAt == null) {
                occurredAt = firstNonBlank(alert.getStartsAt(), alert.getEndsAt(), sentAt);
            }
            String severity = alert.getLabels() == null ? null : alert.getLabels().get("severity");
            var content = teamsContent(
                    profileTag(),
                    alertname,
                    alert.getStatus(),
                    severity,
                    summary,
                    description,
                    occurredAt
            );
            var body = TeamsMessage.builder()
                    .referer("greenlight")
                    .content(content)
                    .notificationType(AlertChannel.TEAMS.name())
                    .targets(targets)
                    .sentAt(sentAt)
                    .build();
            if (!teamsAlertClient.sendWithRetry(body, 3)) {
                continue;
            }
            recordSend(alert, content, targets, actor, ip);
        }
    }

    private void recordSend(
            AlertManagerRequest.Alert alert,
            String message,
            List<String> targets,
            String actor,
            String ip
    ) {
        Map<String, String> labels = alert.getLabels() == null ? Map.of() : alert.getLabels();
        String alertname = firstNonBlank(labels.get("alertname"), "Unknown");
        LocalDateTime sentAt = LocalDateTime.now(ZONE_ID);
        for (String target : targets) {
            AlertSendLog row = AlertSendLog.builder()
                    .fingerprint(firstNonBlank(alert.getFingerprint(), AlertFingerprint.of(labels)))
                    .alertname(alertname)
                    .status(AlertStatus.from(alert.getStatus()))
                    .siteId(blankToNull(labels.get("site_id")))
                    .roomId(blankToNull(labels.get("room_id")))
                    .channel(AlertChannel.TEAMS.name())
                    .target(target)
                    .message(message)
                    .sentAt(sentAt)
                    .createdBy(actor)
                    .createdAt(sentAt)
                    .createdIp(ip)
                    .updatedBy(actor)
                    .updatedAt(sentAt)
                    .updatedIp(ip)
                    .build();
            try {
                alertSendLogMapper.insert(row);
            } catch (Exception exception) {
                log.error("Alert send log insert failed. alertname={} target={}", alertname, target, exception);
            }
        }
    }

    static String teamsContent(
            String profileTag,
            String alertname,
            String status,
            String severity,
            String summary,
            String description,
            String occurredAt
    ) {
        String title = firstNonBlank(summary, alertTitle(alertname));
        String level = severityLabel(status, severity);
        String body = firstNonBlank(description);
        StringBuilder content = new StringBuilder();
        content.append("<b>[Greenlight]").append(profileTag)
                .append(" [").append(level).append("] ")
                .append(title).append("</b>");
        if (body != null) {
            content.append("<br>").append(body.replace("\n", "<br>"));
        }
        content.append("<br>[").append(formatDisplayTime(occurredAt)).append("]");
        return content.toString();
    }

    static String alertTitle(String alertname) {
        if (alertname == null || alertname.isBlank()) {
            return "알림";
        }
        String key = AlertCatalog.subscriptionKey(alertname);
        if (AlertCatalog.isKnown(key)) {
            return AlertCatalog.valueOf(key).label();
        }
        return alertname.trim();
    }

    static String severityLabel(String status, String severity) {
        if (status != null && AlertStatus.RESOLVED.name().equalsIgnoreCase(status.trim())) {
            return "해제";
        }
        if (severity != null && AlertSeverity.CRITICAL.name().equalsIgnoreCase(severity.trim())) {
            return "심각";
        }
        return "경고";
    }

    static String formatDisplayTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String value = raw.trim();
        try {
            return DISPLAY_TIME.format(Instant.parse(value));
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return DISPLAY_TIME.format(OffsetDateTime.parse(value).toInstant());
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.parse(value));
        } catch (DateTimeParseException ignored) {
            return value;
        }
    }

    static String profileTag(String[] activeProfiles) {
        if (activeProfiles == null || activeProfiles.length == 0) {
            return "[default]";
        }
        return "[" + activeProfiles[0] + "]";
    }

    private String profileTag() {
        return profileTag(environment.getActiveProfiles());
    }

    static void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw CoreException.of(ErrorType.INVALID_DATA, "조회 시작 시각은 종료 시각보다 앞서야 합니다.");
        }
        if (Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
            throw CoreException.of(ErrorType.INVALID_DATA, "알람 로그 조회 기간은 최대 7일입니다.");
        }
    }

    private Map<String, String> ensureAlertname(Map<String, String> labels, String alertname) {
        if (labels.containsKey("alertname") && labels.get("alertname") != null && !labels.get("alertname").isBlank()) {
            return labels;
        }
        Map<String, String> copy = new LinkedHashMap<>(labels);
        copy.put("alertname", alertname);
        return copy;
    }

    private LocalDateTime parseTime(String value, LocalDateTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(value).atZone(ZONE_ID).toLocalDateTime();
        } catch (Exception ignored) {
            // fall through
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String writeJson(Map<String, String> value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw CoreException.of(ErrorType.FAILED_TO_PARSE_JSON, "alert labels/annotations JSON 생성에 실패했습니다.");
        }
    }

    private String normalizeActor(String createdBy) {
        String actor = normalize(createdBy);
        return actor == null ? "unknown" : actor;
    }

    private String requestIp() {
        String ip = RequestScopeUtil.getRequestIp();
        return ip == null || ip.isBlank() ? "0.0.0.0" : ip;
    }

    static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
