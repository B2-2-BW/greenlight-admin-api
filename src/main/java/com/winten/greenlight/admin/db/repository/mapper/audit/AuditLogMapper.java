package com.winten.greenlight.admin.db.repository.mapper.audit;

import com.winten.greenlight.admin.domain.audit.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuditLogMapper {
    int insert(AuditLog auditLog);

    List<AuditLog> findPage(
            @Param("targetSiteId") String targetSiteId,
            @Param("createdBy") String createdBy,
            @Param("targetType") String targetType,
            @Param("targetId") String targetId,
            @Param("action") String action,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    long count(
            @Param("targetSiteId") String targetSiteId,
            @Param("createdBy") String createdBy,
            @Param("targetType") String targetType,
            @Param("targetId") String targetId,
            @Param("action") String action,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
