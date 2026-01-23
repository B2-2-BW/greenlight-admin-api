package com.winten.greenlight.admin.db.repository.mapper.site;

import com.winten.greenlight.admin.domain.site.SiteInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface SiteMapper {
    Optional<SiteInfo> findSiteById(SiteInfo siteInfo);
}