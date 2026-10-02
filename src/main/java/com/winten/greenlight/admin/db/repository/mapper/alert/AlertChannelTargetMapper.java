package com.winten.greenlight.admin.db.repository.mapper.alert;

import com.winten.greenlight.admin.domain.alert.AlertChannelTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlertChannelTargetMapper {
    List<AlertChannelTarget> findByAccountId(@Param("accountId") Long accountId);

    int deleteByAccountId(@Param("accountId") Long accountId);

    int insert(AlertChannelTarget target);

    List<String> findEnabledTargets(
            @Param("alertname") String alertname,
            @Param("siteId") String siteId,
            @Param("channel") String channel
    );
}
