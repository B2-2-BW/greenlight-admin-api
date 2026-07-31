package com.winten.greenlight.admin.domain.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuditLogPage {
    private final List<AuditLog> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
}
