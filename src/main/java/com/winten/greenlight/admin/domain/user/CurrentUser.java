package com.winten.greenlight.admin.domain.user;

import lombok.*;

import java.util.List;

@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@Builder
public class CurrentUser {
    private Long accountId;
    private String userId;
    private UserRole userRole;
    private String userSiteId;
    private List<String> accessibleSiteIds;

    public static CurrentUser guest() {
        return CurrentUser.builder().userRole(UserRole.GUEST).build();
    }

    public List<String> resolveAccessibleSiteIds() {
        if (accessibleSiteIds != null && !accessibleSiteIds.isEmpty()) {
            return accessibleSiteIds;
        }
        if (userSiteId == null || userSiteId.isBlank()) {
            return List.of();
        }
        return List.of(userSiteId);
    }

    public boolean canAccessSite(String siteId) {
        return siteId != null && resolveAccessibleSiteIds().contains(siteId);
    }
}