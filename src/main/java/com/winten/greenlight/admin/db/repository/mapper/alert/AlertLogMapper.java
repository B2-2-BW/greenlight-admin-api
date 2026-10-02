package com.winten.greenlight.admin.db.repository.mapper.alert;

import com.winten.greenlight.admin.domain.alert.AlertLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AlertLogMapper {
    int upsertFiring(AlertLog alertLog);

    int resolve(AlertLog alertLog);

    List<AlertLog> findPage(
            @Param("siteId") String siteId,
            @Param("alertname") String alertname,
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    long count(
            @Param("siteId") String siteId,
            @Param("alertname") String alertname,
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
