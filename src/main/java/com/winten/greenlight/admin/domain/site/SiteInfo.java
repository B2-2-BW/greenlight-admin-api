package com.winten.greenlight.admin.domain.site;

import com.winten.greenlight.admin.support.dto.AuditDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class SiteInfo extends AuditDto {
    private String siteId;
    private String siteName;
    private String siteDescription;
    private String siteApiKey;
    private Boolean siteEnabled;
    private Boolean queueEnabled;
    private String deletedBy;
    private LocalDateTime deletedAt;
    private String deletedIp;
}
