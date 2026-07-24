package com.winten.greenlight.admin.api.controller.site;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteInfoRequest {
    @Size(min = 1, max = 255)
    private String siteName;
    @Size(max = 4000)
    private String siteDescription;
    private Boolean siteEnabled;
}
