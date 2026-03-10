package com.winten.greenlight.admin.domain.site;

import com.winten.greenlight.admin.db.repository.mapper.site.SiteMapper;
import com.winten.greenlight.admin.db.repository.redis.site.SiteCacheRepository;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteService {
    private final SiteMapper siteMapper;
    private final SiteCacheRepository siteCacheRepository;

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
            siteCacheRepository.updateSiteApiKeyCache(site);
        }
    }

    public SiteInfo updateSiteInfoById(SiteInfo siteParam) {
        siteMapper.updateSiteInfoById(siteParam); // returning으로 조회 잘 되는지 검사
        var siteInfo = findSiteById(siteParam.getSiteId());
        siteCacheRepository.updateSiteInfo(siteInfo);
        return siteInfo;
    }
}