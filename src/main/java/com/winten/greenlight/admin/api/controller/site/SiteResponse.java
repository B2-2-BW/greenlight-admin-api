package com.winten.greenlight.admin.api.controller.site;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SiteResponse {
    private String siteId;
    private String siteName;
    private String siteDescription;
}