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
        return row;
    }

    @Transactional(readOnly = true)
    public List<AlertSilence> list(String systemCode, boolean activeOnly) {
        AuthUtil.ensureSuper();
        String code = null;
        if (systemCode != null && !systemCode.isBlank()) {
            AlertSilenceSystem system = AlertSilenceSystem.fromCode(systemCode);
            if (system == null) {
                throw CoreException.of(
                        ErrorType.INVALID_DATA,
                        "systemCode는 QUEUE, SCHEDULER, DOCKER, REDIS, HOST, TARGET 중 하나여야 합니다."
                );
            }
            code = system.name();
        }
        return alertSilenceMapper.findOpen(code, activeOnly, LocalDateTime.now(ZONE_ID));
    }

    @Transactional
    public AlertSilence cancel(long silenceId) {
        AuthUtil.ensureSuper();
        AlertSilence existing = alertSilenceMapper.findById(silenceId);
        if (existing == null) {
            throw CoreException.of(ErrorType.SILENCE_NOT_FOUND, "Silence를 찾을 수 없습니다.");
        }
        if (existing.getCancelledAt() != null) {
            return existing;
        }
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        String actor = actor();
        String ip = requestIp();
        int updated = alertSilenceMapper.cancel(silenceId, now, actor, now, ip);
        if (updated == 0) {
            AlertSilence latest = alertSilenceMapper.findById(silenceId);
            if (latest == null) {
                throw CoreException.of(ErrorType.SILENCE_NOT_FOUND, "Silence를 찾을 수 없습니다.");
            }
            return latest;
        }
        existing.setCancelledAt(now);
        existing.setUpdatedBy(actor);
        existing.setUpdatedAt(now);
        existing.setUpdatedIp(ip);
        return existing;
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
