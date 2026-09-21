package com.winten.greenlight.admin.db.repository.mapper.alert;

import com.winten.greenlight.admin.domain.alert.AlertSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlertSubscriptionMapper {
    List<AlertSubscription> findByAccountId(@Param("accountId") Long accountId);

    int upsert(AlertSubscription subscription);
}
