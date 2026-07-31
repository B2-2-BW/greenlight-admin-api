package com.winten.greenlight.admin.api.controller.site;

import com.winten.greenlight.admin.domain.site.SiteConverter;
import com.winten.greenlight.admin.domain.site.SiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Slf4j
@RestController
@RequestMapping("/sites")
@RequiredArgsConstructor
@Validated
public class SiteController {
    private final SiteService siteService;
    private final SiteConverter siteConverter;

    @PostMapping
    public ResponseEntity<SiteCreateResponse> createSite(@RequestBody @Valid SiteCreateRequest request) {
        var site = siteService.createSite(
                request.siteId(),
                request.siteName(),
                request.siteDescription(),
                request.reason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new SiteCreateResponse(
                site.getSiteId(),
                site.getSiteName(),
                site.getSiteDescription(),
                Boolean.TRUE.equals(site.getSiteEnabled()),
                Boolean.TRUE.equals(site.getQueueEnabled()),
                site.getSiteApiKey()
        ));
    }

    @GetMapping("{siteId}")
    public ResponseEntity<SiteResponse> findSiteById(@PathVariable String siteId) {
        var site = siteService.findSiteById(siteId);
        return ResponseEntity.ok(siteConverter.toResponse(site));
    }

    @GetMapping
    public ResponseEntity<SitePageResponse> getSites(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean enabled
    ) {
        var result = siteService.getManageableSites(page, size, query, enabled);
        return ResponseEntity.ok(SitePageResponse.builder()
                .content(result.getContent().stream().map(siteConverter::toResponse).toList())
                .page(result.getPage()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages()).build());
    }

    @GetMapping("{siteId}/manage")
    public ResponseEntity<SiteResponse> getManageableSite(@PathVariable String siteId) {
        return ResponseEntity.ok(siteConverter.toResponse(siteService.getManageableSite(siteId)));
    }

    @PutMapping("/{siteId}")
    public ResponseEntity<SiteResponse> updateSiteInfo(@PathVariable String siteId, @RequestBody @Valid SiteInfoRequest request) {
        var siteParam = siteConverter.toDto(request);
        siteParam.setSiteId(siteId);
        var site = request.hasSiteManagementFields()
                ? siteService.updateSiteInfoById(siteParam, request.isQueueEnabledPresent(), request.getReason())
                : siteService.updateQueueEnabled(siteId, request.getQueueEnabled(), request.getReason());
        return ResponseEntity.ok(siteConverter.toResponse(site));
    }

    @PostMapping("/{siteId}/api-key/rotate")
    public ResponseEntity<SiteApiKeyRotateResponse> rotateSiteApiKey(
            @PathVariable String siteId,
            @RequestBody @Valid SiteApiKeyRotateRequest request
    ) {
        return ResponseEntity.ok(new SiteApiKeyRotateResponse(siteService.rotateSiteApiKey(siteId, request.reason())));
    }

    @DeleteMapping("/{siteId}")
    public ResponseEntity<Void> deleteSite(
            @PathVariable String siteId,
            @RequestBody @Valid SiteDeleteRequest request
    ) {
        siteService.deleteSite(siteId, request.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cache")
    public ResponseEntity<String> findSiteByCache() {
        siteService.reloadSiteCache();
        return ResponseEntity.ok("site cache reload successful");
    }
}
