package com.winten.greenlight.admin.domain.site;

import com.winten.greenlight.admin.db.repository.mapper.site.SiteMapper;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteService {
    private final SiteMapper siteMapper;
    private final SiteCacheManager siteCacheManager;

    public SiteInfo findSiteById(String siteId) {
        var param = SiteInfo.builder().siteId(siteId).build();
        // 유효한 Site ID인지 검증
        return siteMapper.findSiteById(param)
                .orElseThrow(() -> CoreException.of(ErrorType.SITE_NOT_FOUND, "사이트 ID를 찾을 수 없습니다. " + siteId));
    }

    public void reloadSiteCache() {
        AuthUtil.ensureSuper();
        var siteList = siteMapper.findAllSite();
        for (var site : siteList) {
            siteCacheManager.updateSiteApiKeyCache(site);
        }
    }
}