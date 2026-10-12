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

    List<AlertSilence> findPage(
            @Param("systemCode") String systemCode,
            @Param("status") String status,
            @Param("now") LocalDateTime now,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    long count(
            @Param("systemCode") String systemCode,
            @Param("status") String status,
            @Param("now") LocalDateTime now
    );

    int update(
            @Param("silenceId") long silenceId,
            @Param("startsAt") LocalDateTime startsAt,
            @Param("endsAt") LocalDateTime endsAt,
            @Param("comment") String comment,
            @Param("updatedBy") String updatedBy,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedIp") String updatedIp,
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
