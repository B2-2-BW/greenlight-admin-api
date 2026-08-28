package com.winten.greenlight.admin.domain.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSite {
    @JsonIgnore
    private Long accountId;
    private String siteId;
    private String siteName;
    private Boolean siteEnabled;
}
