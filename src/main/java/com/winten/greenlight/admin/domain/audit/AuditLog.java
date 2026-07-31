package com.winten.greenlight.admin.domain.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private Long auditId;
    private String requestId;
    private String sourcePath;
    private String targetSiteId;
    private String targetType;
    private String targetId;
    private String action;
    private String reason;
    private String changeDetail;
    private String createdBy;
    private LocalDateTime createdAt;
    private String createdIp;
}
