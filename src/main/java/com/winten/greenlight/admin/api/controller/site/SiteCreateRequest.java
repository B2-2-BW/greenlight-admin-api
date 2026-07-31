package com.winten.greenlight.admin.api.controller.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SiteCreateRequest(
        @NotBlank
        @Size(max = 4)
        @Pattern(regexp = "[A-Za-z0-9_-]+")
        String siteId,
        @NotBlank
        @Size(max = 255)
        String siteName,
        @Size(max = 4000)
        String siteDescription,
        @NotBlank
        @Size(max = 1000)
        String reason
) {
}
