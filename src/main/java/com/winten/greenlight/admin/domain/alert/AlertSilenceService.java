package com.winten.greenlight.admin.domain.alert;

import com.winten.greenlight.admin.db.repository.mapper.alert.AlertSilenceMapper;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import com.winten.greenlight.admin.support.util.RequestScopeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertSilenceService {
    static final int MAX_COMMENT_LENGTH = 500;
    static final ZoneId ZONE_ID = AlertService.ZONE_ID;

    private final AlertSilenceMapper alertSilenceMapper;

    @Transactional
    public AlertSilence create(String systemCode, LocalDateTime startsAt, LocalDateTime endsAt, String comment) {
        AuthUtil.ensureSuper();
        AlertSilenceSystem system = AlertSilenceSystem.fromCode(systemCode);
        if (system == null) {
            throw CoreException.of(
                    ErrorType.INVALID_DATA,
                    "systemCode는 QUEUE, SCHEDULER, DOCKER, REDIS, HOST, TARGET 중 하나여야 합니다."
            );
        }
        if (startsAt == null || endsAt == null) {
            throw CoreException.of(ErrorType.INVALID_DATA, "Silence 시작 시각과 종료 시각은 필수입니다.");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw CoreException.of(ErrorType.INVALID_DATA, "Silence 종료 시각은 시작 시각보다 뒤여야 합니다.");
        }
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        if (!endsAt.isAfter(now)) {
            throw CoreException.of(ErrorType.INVALID_DATA, "Silence 종료 시각은 현재 시각보다 뒤여야 합니다.");
        }
        String normalizedComment = normalizeComment(comment);
        String actor = actor();
        String ip = requestIp();
        AlertSilence row = AlertSilence.builder()
                .systemCode(system.name())
                .startsAt(startsAt)
                .endsAt(endsAt)
                .comment(normalizedComment)
                .createdBy(actor)
                .createdAt(now)
                .createdIp(ip)
                .updatedBy(actor)
                .updatedAt(now)
                .updatedIp(ip)
                .build();
        alertSilenceMapper.insert(row);
        return fillStatus(row, now);
    }

    @Transactional(readOnly = true)
    public AlertSilencePage list(String systemCode, String status, int requestedPage, int size) {
        String code = normalizeSystemCode(systemCode);
        String normalizedStatus = normalizeStatus(status);
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        int pageSize = Math.min(Math.max(size, 1), 100);
        long totalElements = alertSilenceMapper.count(code, normalizedStatus, now);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
        int page = totalPages == 0 ? 1 : Math.min(Math.max(requestedPage, 1), totalPages);
        List<AlertSilence> content = totalElements == 0
                ? List.of()
                : alertSilenceMapper.findPage(
                        code, normalizedStatus, now, pageSize, (long) (page - 1) * pageSize
                );
        content.forEach(row -> fillStatus(row, now));
        return new AlertSilencePage(content, page, pageSize, totalElements, totalPages);
    }

    @Transactional
    public AlertSilence update(long silenceId, LocalDateTime startsAt, LocalDateTime endsAt, String comment) {
        AuthUtil.ensureSuper();
        AlertSilence existing = alertSilenceMapper.findById(silenceId);
        if (existing == null) {
            throw CoreException.of(ErrorType.SILENCE_NOT_FOUND, "Silence를 찾을 수 없습니다.");
        }
        if (existing.getCancelledAt() != null) {
            throw CoreException.of(ErrorType.INVALID_DATA, "취소된 Silence는 수정할 수 없습니다.");
        }
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        if (existing.getEndsAt() == null || !existing.getEndsAt().isAfter(now)) {
            throw CoreException.of(ErrorType.INVALID_DATA, "종료된 Silence는 수정할 수 없습니다.");
        }
        boolean started = existing.getStartsAt() != null && !existing.getStartsAt().isAfter(now);
        LocalDateTime nextStart = started ? existing.getStartsAt() : startsAt;
        if (nextStart == null || endsAt == null) {
            throw CoreException.of(ErrorType.INVALID_DATA, "Silence 시작 시각과 종료 시각은 필수입니다.");
        }
        if (!endsAt.isAfter(nextStart)) {
            throw CoreException.of(ErrorType.INVALID_DATA, "Silence 종료 시각은 시작 시각보다 뒤여야 합니다.");
        }
        if (!endsAt.isAfter(now)) {
            throw CoreException.of(ErrorType.INVALID_DATA, "Silence 종료 시각은 현재 시각보다 뒤여야 합니다.");
        }
        String normalizedComment = normalizeComment(comment);
        String actor = actor();
        String ip = requestIp();
        int updated = alertSilenceMapper.update(
                silenceId, nextStart, endsAt, normalizedComment, actor, now, ip, now
        );
        if (updated == 0) {
            AlertSilence latest = alertSilenceMapper.findById(silenceId);
            if (latest == null) {
                throw CoreException.of(ErrorType.SILENCE_NOT_FOUND, "Silence를 찾을 수 없습니다.");
            }
            if (latest.getCancelledAt() != null) {
                throw CoreException.of(ErrorType.INVALID_DATA, "취소된 Silence는 수정할 수 없습니다.");
            }
            throw CoreException.of(ErrorType.INVALID_DATA, "종료된 Silence는 수정할 수 없습니다.");
        }
        existing.setStartsAt(nextStart);
        existing.setEndsAt(endsAt);
        existing.setComment(normalizedComment);
        existing.setUpdatedBy(actor);
        existing.setUpdatedAt(now);
        existing.setUpdatedIp(ip);
        return fillStatus(existing, now);
    }

    @Transactional
    public AlertSilence cancel(long silenceId) {
        AuthUtil.ensureSuper();
        AlertSilence existing = alertSilenceMapper.findById(silenceId);
        if (existing == null) {
            throw CoreException.of(ErrorType.SILENCE_NOT_FOUND, "Silence를 찾을 수 없습니다.");
        }
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        if (existing.getCancelledAt() != null) {
            return fillStatus(existing, now);
        }
        String actor = actor();
        String ip = requestIp();
        int updated = alertSilenceMapper.cancel(silenceId, now, actor, now, ip);
        if (updated == 0) {
            AlertSilence latest = alertSilenceMapper.findById(silenceId);
            if (latest == null) {
                throw CoreException.of(ErrorType.SILENCE_NOT_FOUND, "Silence를 찾을 수 없습니다.");
            }
            return fillStatus(latest, now);
        }
        existing.setCancelledAt(now);
        existing.setUpdatedBy(actor);
        existing.setUpdatedAt(now);
        existing.setUpdatedIp(ip);
        return fillStatus(existing, now);
    }

    public Set<AlertSilenceSystem> activeSystems(LocalDateTime now) {
        LocalDateTime at = now == null ? LocalDateTime.now(ZONE_ID) : now;
        List<String> codes = alertSilenceMapper.findActiveSystemCodes(at);
        if (codes == null || codes.isEmpty()) {
            return Set.of();
        }
        Set<AlertSilenceSystem> systems = EnumSet.noneOf(AlertSilenceSystem.class);
        for (String code : codes) {
            AlertSilenceSystem system = AlertSilenceSystem.fromCode(code);
            if (system != null) {
                systems.add(system);
            }
        }
        return systems;
    }

    private static String normalizeSystemCode(String systemCode) {
        if (systemCode == null || systemCode.isBlank()) {
            return null;
        }
        AlertSilenceSystem system = AlertSilenceSystem.fromCode(systemCode);
        if (system == null) {
            throw CoreException.of(
                    ErrorType.INVALID_DATA,
                    "systemCode는 QUEUE, SCHEDULER, DOCKER, REDIS, HOST, TARGET 중 하나여야 합니다."
            );
        }
        return system.name();
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVE", "SCHEDULED", "ENDED", "CANCELLED").contains(normalized)) {
            throw CoreException.of(
                    ErrorType.INVALID_DATA,
                    "status는 ACTIVE, SCHEDULED, ENDED, CANCELLED 중 하나여야 합니다."
            );
        }
        return normalized;
    }

    private static AlertSilence fillStatus(AlertSilence row, LocalDateTime now) {
        if (row.getCancelledAt() != null) {
            row.setStatus("CANCELLED");
        } else if (row.getStartsAt() != null && row.getStartsAt().isAfter(now)) {
            row.setStatus("SCHEDULED");
        } else if (row.getEndsAt() == null || !row.getEndsAt().isAfter(now)) {
            row.setStatus("ENDED");
        } else {
            row.setStatus("ACTIVE");
        }
        return row;
    }

    private static String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String trimmed = comment.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_COMMENT_LENGTH) {
            throw CoreException.of(ErrorType.INVALID_DATA, "comment는 500자를 넘을 수 없습니다.");
        }
        return trimmed;
    }

    private static String actor() {
        String userId = AuthUtil.getCurrentUser().getUserId();
        if (userId == null || userId.isBlank()) {
            return "unknown";
        }
        return userId.trim();
    }

    private static String requestIp() {
        String ip = RequestScopeUtil.getRequestIp();
        return ip == null || ip.isBlank() ? "0.0.0.0" : ip;
    }
}
