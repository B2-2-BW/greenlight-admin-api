package com.winten.greenlight.admin.api.controller.site;

import lombok.AllArgsConstructor;
import lombok.Data;

/** The generated key is deliberately returned only by the rotation endpoint. */
@Data
@AllArgsConstructor
public class SiteApiKeyRotateResponse {
    private String apiKey;
}
