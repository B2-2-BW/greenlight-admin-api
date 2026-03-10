package com.winten.greenlight.admin.api.controller.site;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteInfoRequest {
    private String siteName;
    private String siteDescription;
    private String siteApiKey;
    private boolean siteEnabled;
}