package com.winten.greenlight.admin.db.repository.mapper.site;

import com.winten.greenlight.admin.domain.site.SiteInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SiteMapper {
    // TODO public 하게 모두가 쓸 수 있는 API임을 함수명에 명시해야함
    Optional<SiteInfo> findSiteById(SiteInfo siteInfo);
    List<SiteInfo> findSitesPage(@Param("siteIds") List<String> siteIds, @Param("query") String query,
                                 @Param("enabled") Boolean enabled, @Param("limit") int limit, @Param("offset") long offset);
    long countSites(@Param("siteIds") List<String> siteIds, @Param("query") String query, @Param("enabled") Boolean enabled);
    int updateSiteInfoById(SiteInfo siteInfo);
    int updateSiteApiKey(SiteInfo siteInfo);
    int insertSite(SiteInfo siteInfo);
    int softDeleteSite(SiteInfo siteInfo);
    boolean existsBySiteIdIncludingDeleted(@Param("siteId") String siteId);
    boolean existsBySiteApiKey(@Param("siteApiKey") String siteApiKey);
    List<SiteInfo> findAllSite();
}
