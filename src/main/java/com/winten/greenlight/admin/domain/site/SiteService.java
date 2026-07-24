package com.winten.greenlight.admin.domain.site;

import com.winten.greenlight.admin.db.repository.mapper.site.SiteMapper;
import com.winten.greenlight.admin.db.repository.redis.site.SiteCacheRepository;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteService {
    private final SiteMapper siteMapper;
    private final SiteCacheRepository siteCacheRepository;
    private final SiteApiKeyGenerator siteApiKeyGenerator;

    public SiteInfo findSiteById(String siteId) {
        var param = SiteInfo.builder().siteId(siteId).build();
        // 유효한 Site ID인지 검증
        return siteMapper.findSiteById(param)
                .orElseThrow(() -> CoreException.of(ErrorType.SITE_NOT_FOUND, "사이트 ID를 찾을 수 없습니다. " + siteId));
    }

    public void reloadSiteCache() {
        AuthUtil.ensureUserAdmin();
        var currentUser = AuthUtil.getCurrentUser();
        List<SiteInfo> siteList = currentUser.getUserRole().isSuper()
                ? siteMapper.findAllSite()
                : List.of(findSiteById(currentUser.getUserSiteId()));
        for (var site : siteList) {
            siteCacheRepository.updateSiteApiKeyCache(site);
            siteCacheRepository.updateSiteInfo(site);
        }
    }

    public SitePage getManageableSites(int requestedPage, int size, String query, Boolean enabled) {
        AuthUtil.ensureUserAdmin();
        var currentUser = AuthUtil.getCurrentUser();
        String siteId = currentUser.getUserRole().isSuper() ? null : currentUser.getUserSiteId();
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        long totalElements = siteMapper.countSites(siteId, normalizedQuery, enabled);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int page = totalPages == 0 ? 1 : Math.min(requestedPage, totalPages);
        var content = totalElements == 0 ? List.<SiteInfo>of()
                : siteMapper.findSitesPage(siteId, normalizedQuery, enabled, size, (long) (page - 1) * size);
        return new SitePage(content, page, size, totalElements, totalPages);
    }

    public SiteInfo getManageableSite(String siteId) {
        AuthUtil.ensureUserAdmin();
        AuthUtil.ensureCanManageSite(siteId);
        return findSiteById(siteId);
    }

    public SiteInfo updateSiteInfoById(SiteInfo siteParam) {
        AuthUtil.ensureUserAdmin();
        AuthUtil.ensureCanManageSite(siteParam.getSiteId());
        var currentUser = AuthUtil.getCurrentUser();
        // The regular site update flow must never alter credentials, including from an internal caller.
        siteParam.setSiteApiKey(null);
        siteParam.setUpdatedBy(currentUser.getUserId());
        if (siteMapper.updateSiteInfoById(siteParam) == 0) {
            throw CoreException.of(ErrorType.SITE_NOT_FOUND, "사이트 ID를 찾을 수 없습니다. " + siteParam.getSiteId());
        }
        var siteInfo = findSiteById(siteParam.getSiteId());
        siteCacheRepository.updateSiteInfo(siteInfo);
        return siteInfo;
    }

    public String rotateSiteApiKey(String siteId) {
        AuthUtil.ensureSuper();
        var previousSite = findSiteById(siteId);
        String apiKey = generateAvailableApiKey();
        var update = SiteInfo.builder()
                .siteId(siteId)
                .siteApiKey(apiKey)
                .updatedBy(AuthUtil.getCurrentUser().getUserId())
                .build();
        if (siteMapper.updateSiteApiKey(update) == 0) {
            throw CoreException.of(ErrorType.SITE_NOT_FOUND, "사이트 ID를 찾을 수 없습니다. " + siteId);
        }
        var updatedSite = findSiteById(siteId);
        siteCacheRepository.deleteSiteApiKeyCache(previousSite.getSiteApiKey());
        siteCacheRepository.updateSiteApiKeyCache(updatedSite);
        siteCacheRepository.updateSiteInfo(updatedSite);
        return apiKey;
    }

    private String generateAvailableApiKey() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = siteApiKeyGenerator.generate();
            if (!siteMapper.existsBySiteApiKey(candidate)) return candidate;
        }
        throw CoreException.of(ErrorType.DEFAULT_ERROR, "API 키 생성에 실패했습니다.");
    }
}
