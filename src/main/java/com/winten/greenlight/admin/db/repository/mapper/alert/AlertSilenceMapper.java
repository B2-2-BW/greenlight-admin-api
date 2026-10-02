package com.winten.greenlight.admin.db.repository.mapper.alert;

import com.winten.greenlight.admin.domain.alert.AlertSilence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AlertSilenceMapper {
    int insert(AlertSilence alertSilence);

    AlertSilence findById(@Param("silenceId") long silenceId);

    List<AlertSilence> findOpen(
            @Param("systemCode") String systemCode,
            @Param("activeOnly") boolean activeOnly,
            @Param("now") LocalDateTime now
    );

    List<String> findActiveSystemCodes(@Param("now") LocalDateTime now);

    int cancel(
            @Param("silenceId") long silenceId,
            @Param("cancelledAt") LocalDateTime cancelledAt,
            @Param("updatedBy") String updatedBy,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedIp") String updatedIp
    );
}
