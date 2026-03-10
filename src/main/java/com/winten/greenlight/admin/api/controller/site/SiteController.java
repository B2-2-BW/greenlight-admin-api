package com.winten.greenlight.admin.api.controller.site;

import com.winten.greenlight.admin.domain.site.SiteConverter;
import com.winten.greenlight.admin.domain.site.SiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/sites")
@RequiredArgsConstructor
public class SiteController {
    private final SiteService siteService;
    private final SiteConverter siteConverter;

    @GetMapping("{siteId}")
    public ResponseEntity<SiteResponse> findSiteById(@PathVariable String siteId) {
        var site = siteService.findSiteById(siteId);
        return ResponseEntity.ok(siteConverter.toResponse(site));
    }

    @PutMapping("/{siteId}")
    public ResponseEntity<SiteResponse> updateSiteInfo(@PathVariable String siteId, @RequestBody SiteInfoRequest request) {
        var siteParam = siteConverter.toDto(request);
        siteParam.setSiteId(siteId);
        var site = siteService.updateSiteInfoById(siteParam);
        return ResponseEntity.ok(siteConverter.toResponse(site));
    }

    @PostMapping("/cache")
    public ResponseEntity<String> findSiteByCache() {
        siteService.reloadSiteCache();
        return ResponseEntity.ok("site cache reload successful");
    }
}