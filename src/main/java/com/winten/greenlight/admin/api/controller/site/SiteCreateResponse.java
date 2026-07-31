package com.winten.greenlight.admin.api.controller.site;

public record SiteCreateResponse(
        String siteId,
        String siteName,
        String siteDescription,
        boolean siteEnabled,
        boolean queueEnabled,
        String apiKey
) {
}
