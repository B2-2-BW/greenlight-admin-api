package com.winten.greenlight.admin.domain.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winten.greenlight.admin.support.dto.AuditDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class User extends AuditDto {
    private Long accountId;
    private String userId;
    private String siteId;
    private String siteName;
    private List<String> siteIds;
    private List<UserSite> sites;
    private String username;
    private String profileColor;
    private String profileInitials;
    private String userEmail;
    private Boolean passwordResetRequired;
    private AccountStatus accountStatus;
    @JsonIgnore
    private LocalDateTime passwordChangedAt;
    private String phoneNumber;
    @JsonIgnore
    private String password;
    @JsonIgnore
    private String passwordHash;
    private UserRole userRole;
    private Boolean autoLogin;

    public List<String> resolveSiteIds() {
        if (siteIds == null || siteIds.isEmpty()) {
            return List.of();
        }
        return siteIds.stream().filter(id -> id != null && !id.isBlank()).toList();
    }

    /**
     * user_account.site_id는 홈 사이트(표시/감사 기본값)일 뿐 권한이 아니다.
     * 비어 있으면 부여된 사이트 중 첫 값을 쓴다.
     */
    public String resolveHomeSiteId() {
        if (siteId != null && !siteId.isBlank()) {
            return siteId;
        }
        List<String> granted = resolveSiteIds();
        return granted.isEmpty() ? null : granted.get(0);
    }

    public boolean hasSiteAccess(String targetSiteId) {
        return targetSiteId != null && resolveSiteIds().contains(targetSiteId);
    }
}
