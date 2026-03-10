package com.winten.greenlight.admin.db.repository.mapper.site;

import com.winten.greenlight.admin.domain.site.SiteInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SiteMapper {
    // TODO public 하게 모두가 쓸 수 있는 API임을 함수명에 명시해야함
    Optional<SiteInfo> findSiteById(SiteInfo siteInfo);
    SiteInfo updateSiteInfoById(SiteInfo siteInfo);
    List<SiteInfo> findAllSite();
}