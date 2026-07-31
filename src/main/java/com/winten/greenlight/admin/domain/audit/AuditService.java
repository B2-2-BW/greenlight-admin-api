package com.winten.greenlight.admin.domain.audit;

import com.winten.greenlight.admin.db.repository.mapper.audit.AuditLogMapper;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import com.winten.greenlight.admin.support.util.RequestScopeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogMapper auditLogMapper;
    private final JsonMapper jsonMapper;

    @Transactional
    public boolean recordChanges(
            String targetSiteId,
            String targetType,
            String targetId,
            AuditAction action,
            String reason,
            Map<String, ?> before,
            Map<String, ?> after,
            List<String> allowedFields
    ) {
        String normalizedReason = normalizeReason(reason);
        if (action == null) {
            throw CoreException.of(ErrorType.INVALID_DATA, "감사로그 작업 유형이 필요합니다.");
        }
        Map<String, Object> delta = createDelta(before, after, allowedFields);
        if (delta.isEmpty()) return false;

        String requestId = RequestScopeUtil.getRequestId();
        AuditLog auditLog = AuditLog.builder()
                .requestId(requestId == null ? UUID.randomUUID().toString() : requestId)
                .sourcePath(RequestScopeUtil.getSourcePath())
                .targetSiteId(targetSiteId)
                .targetType(targetType)
                .targetId(targetId)
                .action(action.name())
                .reason(normalizedReason)
                .changeDetail(writeDelta(delta))
                .build();
        return auditLogMapper.insert(auditLog) == 1;
    }

    @Transactional(readOnly = true)
    public AuditLogPage getAuditLogs(
            int requestedPage,
            int size,
            String requestedSiteId,
            String createdBy,
            String targetType,
            String targetId,
            AuditAction action,
            LocalDateTime from,
            LocalDateTime to
    ) {
        AuthUtil.ensureUserAdmin();
        if (from != null && to != null && from.isAfter(to)) {
            throw CoreException.of(ErrorType.INVALID_DATA, "조회 시작 시각은 종료 시각보다 늦을 수 없습니다.");
        }
        var currentUser = AuthUtil.getCurrentUser();
        String targetSiteId = currentUser.getUserRole().isSuper()
                ? normalize(requestedSiteId)
                : currentUser.getUserSiteId();
        String normalizedCreatedBy = normalize(createdBy);
        String normalizedTargetType = normalize(targetType);
        String normalizedTargetId = normalize(targetId);
        String normalizedAction = action == null ? null : action.name();

        long totalElements = auditLogMapper.count(
                targetSiteId, normalizedCreatedBy, normalizedTargetType, normalizedTargetId,
                normalizedAction, from, to
        );
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int page = totalPages == 0 ? 1 : Math.min(requestedPage, totalPages);
        List<AuditLog> content = totalElements == 0
                ? List.of()
                : auditLogMapper.findPage(
                        targetSiteId, normalizedCreatedBy, normalizedTargetType, normalizedTargetId,
                        normalizedAction, from, to, size, (long) (page - 1) * size
                );
        return new AuditLogPage(content, page, size, totalElements, totalPages);
    }

    private Map<String, Object> createDelta(
            Map<String, ?> before,
            Map<String, ?> after,
            List<String> allowedFields
    ) {
        Map<String, Object> delta = new LinkedHashMap<>();
        for (String field : allowedFields) {
            Object previousValue = before.get(field);
            Object nextValue = after.get(field);
            if (!Objects.equals(previousValue, nextValue)) {
                Map<String, Object> change = new LinkedHashMap<>();
                change.put("before", previousValue);
                change.put("after", nextValue);
                delta.put(field, change);
            }
        }
        return delta;
    }

    private String writeDelta(Map<String, Object> delta) {
        try {
            return jsonMapper.writeValueAsString(delta);
        } catch (Exception exception) {
            throw CoreException.of(ErrorType.DEFAULT_ERROR, "감사로그 변경내역 생성에 실패했습니다.");
        }
    }

    private String normalizeReason(String reason) {
        String normalized = normalize(reason);
        if (normalized == null) return "";
        if (normalized.length() > 1000) {
            throw CoreException.of(ErrorType.INVALID_DATA, "변경 사유는 1000자 이하여야 합니다.");
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
