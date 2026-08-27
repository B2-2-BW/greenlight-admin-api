package com.winten.greenlight.admin.domain.site;

import com.winten.greenlight.admin.db.repository.mapper.site.SiteMapper;
import com.winten.greenlight.admin.db.repository.mapper.user.UserMapper;
import com.winten.greenlight.admin.db.repository.redis.site.SiteCacheRepository;
import com.winten.greenlight.admin.domain.audit.AuditAction;
import com.winten.greenlight.admin.domain.audit.AuditService;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SiteService {
    private final SiteMapper siteMapper;
    private final SiteCacheRepository siteCacheRepository;
    private final SiteApiKeyGenerator siteApiKeyGenerator;
    private final AuditService auditService;
    private final UserMapper userMapper;

    private static final List<String> AUDITED_SITE_FIELDS = List.of(
            "siteName", "siteDescription", "siteEnabled", "queueEnabled"
    );

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
                : currentUser.resolveAccessibleSiteIds().stream()
                        .map(this::findSiteById)
                        .toList();
        for (var site : siteList) {
            siteCacheRepository.updateSiteApiKeyCache(site);
            siteCacheRepository.updateSiteInfo(site);
        }
    }

    public SitePage getManageableSites(int requestedPage, int size, String query, Boolean enabled) {
        AuthUtil.ensureUserAdmin();
        var currentUser = AuthUtil.getCurrentUser();
        List<String> siteIds = currentUser.getUserRole().isSuper() ? null : currentUser.resolveAccessibleSiteIds();
        if (siteIds != null && siteIds.isEmpty()) {
            return new SitePage(List.of(), 1, size, 0, 0);
        }
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        long totalElements = siteMapper.countSites(siteIds, normalizedQuery, enabled);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int page = totalPages == 0 ? 1 : Math.min(requestedPage, totalPages);
        var content = totalElements == 0 ? List.<SiteInfo>of()
                : siteMapper.findSitesPage(siteIds, normalizedQuery, enabled, size, (long) (page - 1) * size);
        return new SitePage(content, page, size, totalElements, totalPages);
    }

    public SiteInfo getManageableSite(String siteId) {
        AuthUtil.ensureUserAdmin();
        AuthUtil.ensureCanManageSite(siteId);
        return findSiteById(siteId);
    }

    @Transactional
    public SiteInfo createSite(
            String siteId,
            String siteName,
            String siteDescription,
            String reason
    ) {
        AuthUtil.ensureSuper();
        if (siteMapper.existsBySiteIdIncludingDeleted(siteId)) {
            throw CoreException.of(ErrorType.SITE_EXISTS, "이미 사용된 사이트 ID입니다. " + siteId);
        }
        SiteInfo siteInfo = SiteInfo.builder()
                .siteId(siteId)
                .siteName(siteName.trim())
                .siteDescription(siteDescription == null || siteDescription.isBlank() ? null : siteDescription.trim())
                .siteApiKey(generateAvailableApiKey())
                .siteEnabled(true)
                .queueEnabled(false)
                .build();
        if (siteMapper.insertSite(siteInfo) != 1) {
            throw CoreException.of(ErrorType.DEFAULT_ERROR, "사이트 생성에 실패했습니다.");
        }

        auditService.recordChanges(
                siteId,
                "SITE",
                siteId,
                AuditAction.CREATE,
                reason,
                emptyAuditedValues(),
                auditedValues(siteInfo),
                AUDITED_SITE_FIELDS
        );

        siteCacheRepository.updateSiteApiKeyCache(siteInfo);
        siteCacheRepository.updateSiteInfo(siteInfo);
        siteCacheRepository.updateRoomListCache(siteId, List.of());
        return siteInfo;
    }

    @Transactional
    public void deleteSite(String siteId, String reason) {
        AuthUtil.ensureSuper();
        SiteInfo previousSite = findSiteById(siteId);
        if (siteMapper.softDeleteSite(SiteInfo.builder().siteId(siteId).build()) != 1) {
            throw CoreException.of(ErrorType.SITE_NOT_FOUND, "사이트 ID를 찾을 수 없습니다. " + siteId);
        }
        userMapper.deleteSiteAccessBySiteId(siteId);
        userMapper.reassignHomeSiteIfMissing(siteId);
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("siteEnabled", previousSite.getSiteEnabled());
        before.put("queueEnabled", previousSite.getQueueEnabled());
        before.put("deleted", false);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("siteEnabled", false);
        after.put("queueEnabled", false);
        after.put("deleted", true);
        auditService.recordChanges(
                siteId,
                "SITE",
                siteId,
                AuditAction.DELETE,
                reason,
                before,
                after,
                List.of("siteEnabled", "queueEnabled", "deleted")
        );

        SiteInfo disabledSite = SiteInfo.builder()
                .siteId(siteId)
                .siteEnabled(false)
                .queueEnabled(false)
                .build();
        siteCacheRepository.deleteSiteApiKeyCache(previousSite.getSiteApiKey());
        siteCacheRepository.updateSiteInfo(disabledSite);
        siteCacheRepository.updateRoomListCache(siteId, List.of());
    }

    @Transactional
    public SiteInfo updateSiteInfoById(
            SiteInfo siteParam,
            boolean siteEnabledPresent,
            boolean queueEnabledPresent,
            String reason
    ) {
        AuthUtil.ensureUserAdmin();
        AuthUtil.ensureCanManageSite(siteParam.getSiteId());
        if (siteEnabledPresent || siteParam.getSiteEnabled() != null) {
            AuthUtil.ensureSuper();
        }
        if (siteEnabledPresent && siteParam.getSiteEnabled() == null) {
            throw CoreException.of(ErrorType.INVALID_DATA, "siteEnabled 값은 null일 수 없습니다.");
        }
        if (queueEnabledPresent && siteParam.getQueueEnabled() == null) {
            throw CoreException.of(ErrorType.INVALID_DATA, "queueEnabled 값은 null일 수 없습니다.");
        }
        if (siteParam.getSiteName() == null
                && siteParam.getSiteDescription() == null
                && siteParam.getSiteEnabled() == null
                && siteParam.getQueueEnabled() == null) {
            throw CoreException.of(ErrorType.INVALID_DATA, "수정할 사이트 정보가 없습니다.");
        }
        if (siteParam.getSiteName() != null) {
            String siteName = siteParam.getSiteName().trim();
            if (siteName.isEmpty()) {
                throw CoreException.of(ErrorType.INVALID_DATA, "사이트명을 입력해 주세요.");
            }
            siteParam.setSiteName(siteName);
        }
        if (siteParam.getSiteDescription() != null) {
            siteParam.setSiteDescription(siteParam.getSiteDescription().trim());
        }

        return updateSiteInfo(siteParam, reason);
    }

    @Transactional
    public SiteInfo updateQueueEnabled(String siteId, Boolean queueEnabled, String reason) {
        AuthUtil.ensureUserAdmin();
        AuthUtil.ensureCanManageSite(siteId);
        if (queueEnabled == null) {
            throw CoreException.of(ErrorType.INVALID_DATA, "queueEnabled 값은 null일 수 없습니다.");
        }
        return updateSiteInfo(SiteInfo.builder()
                .siteId(siteId)
                .queueEnabled(queueEnabled)
                .build(), reason);
    }

    private SiteInfo updateSiteInfo(SiteInfo siteParam, String reason) {
        var currentUser = AuthUtil.getCurrentUser();
        var previousSite = findSiteById(siteParam.getSiteId());
        // The regular site update flow must never alter credentials, including from an internal caller.
        siteParam.setSiteApiKey(null);
        siteParam.setUpdatedBy(currentUser.getUserId());
        if (siteMapper.updateSiteInfoById(siteParam) == 0) {
            throw CoreException.of(ErrorType.SITE_NOT_FOUND, "사이트 ID를 찾을 수 없습니다. " + siteParam.getSiteId());
        }
        var siteInfo = findSiteById(siteParam.getSiteId());
        siteCacheRepository.updateSiteInfo(siteInfo);
        auditService.recordChanges(
                siteInfo.getSiteId(),
                "SITE",
                siteInfo.getSiteId(),
                AuditAction.UPDATE,
                reason,
                auditedValues(previousSite),
                auditedValues(siteInfo),
                AUDITED_SITE_FIELDS
        );
        return siteInfo;
    }

    @Transactional
    public String rotateSiteApiKey(String siteId, String reason) {
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
        auditService.recordChanges(
                siteId,
                "SITE",
                siteId,
                AuditAction.UPDATE,
                reason,
                Map.of("apiKeyRotated", false),
                Map.of("apiKeyRotated", true),
                List.of("apiKeyRotated")
        );
        return apiKey;
    }

    private Map<String, Object> auditedValues(SiteInfo siteInfo) {
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        values.put("siteName", siteInfo.getSiteName());
        values.put("siteDescription", siteInfo.getSiteDescription());
        values.put("siteEnabled", siteInfo.getSiteEnabled());
        values.put("queueEnabled", siteInfo.getQueueEnabled());
        return values;
    }

    private Map<String, Object> emptyAuditedValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        AUDITED_SITE_FIELDS.forEach(field -> values.put(field, null));
        return values;
    }

    private String generateAvailableApiKey() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = siteApiKeyGenerator.generate();
            if (!siteMapper.existsBySiteApiKey(candidate)) return candidate;
        }
        throw CoreException.of(ErrorType.DEFAULT_ERROR, "API 키 생성에 실패했습니다.");
    }
}
