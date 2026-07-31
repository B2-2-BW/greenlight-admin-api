package com.winten.greenlight.admin.api.controller.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SiteApiKeyRotateRequest(
        @NotBlank @Size(max = 1000) String reason
) {
}
