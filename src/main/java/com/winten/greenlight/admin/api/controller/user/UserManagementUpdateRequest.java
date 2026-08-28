package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserManagementUpdateRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String userEmail;

    @NotEmpty
    private List<@NotBlank String> siteIds;

    /** 이전 API 호환용. 권한의 기준은 siteIds다. */
    private String siteId;

    @NotNull
    private UserRole userRole;

    public void setSiteId(String siteId) {
        this.siteId = siteId;
        if ((siteIds == null || siteIds.isEmpty()) && siteId != null && !siteId.isBlank()) {
            this.siteIds = List.of(siteId.trim());
        }
    }

    public List<String> resolveSiteIds() {
        if (siteIds != null && !siteIds.isEmpty()) {
            return siteIds;
        }
        if (siteId == null || siteId.isBlank()) {
            return List.of();
        }
        return List.of(siteId.trim());
    }
}
