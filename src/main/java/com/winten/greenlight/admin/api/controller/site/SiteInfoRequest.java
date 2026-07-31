package com.winten.greenlight.admin.api.controller.site;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
public class SiteInfoRequest {
    @Size(min = 1, max = 255)
    private String siteName;
    @Size(max = 4000)
    private String siteDescription;
    private Boolean siteEnabled;
    private Boolean queueEnabled;
    @NotBlank
    @Size(max = 1000)
    private String reason;

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean siteManagementFieldsPresent;
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean queueEnabledPresent;

    @JsonSetter("siteName")
    public void setSiteName(String siteName) {
        this.siteName = siteName;
        this.siteManagementFieldsPresent = true;
    }

    @JsonSetter("siteDescription")
    public void setSiteDescription(String siteDescription) {
        this.siteDescription = siteDescription;
        this.siteManagementFieldsPresent = true;
    }

    @JsonSetter("siteEnabled")
    public void setSiteEnabled(Boolean siteEnabled) {
        this.siteEnabled = siteEnabled;
        this.siteManagementFieldsPresent = true;
    }

    @JsonSetter("queueEnabled")
    public void setQueueEnabled(Boolean queueEnabled) {
        this.queueEnabled = queueEnabled;
        this.queueEnabledPresent = true;
    }

    @JsonIgnore
    public boolean hasSiteManagementFields() {
        return siteManagementFieldsPresent;
    }

    @JsonIgnore
    public boolean isQueueEnabledPresent() {
        return queueEnabledPresent;
    }
}
