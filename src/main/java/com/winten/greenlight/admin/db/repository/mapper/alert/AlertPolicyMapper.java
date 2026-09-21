package com.winten.greenlight.admin.db.repository.mapper.alert;

import com.winten.greenlight.admin.domain.alert.AlertPolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlertPolicyMapper {
    AlertPolicy findBySiteId(@Param("siteId") String siteId);

    List<AlertPolicy> findAllLive();

    int upsert(AlertPolicy policy);
}
