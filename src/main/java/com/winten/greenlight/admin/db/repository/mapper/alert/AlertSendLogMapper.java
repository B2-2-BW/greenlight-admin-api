package com.winten.greenlight.admin.db.repository.mapper.alert;

import com.winten.greenlight.admin.domain.alert.AlertSendLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlertSendLogMapper {
    int insert(AlertSendLog alertSendLog);
}
