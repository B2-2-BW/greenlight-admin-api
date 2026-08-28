package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserApprovalRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String userEmail;

    @NotEmpty
    private List<@NotBlank String> siteIds;

    /**
     * 이전 승인 API 호환용. 권한의 기준은 {@link #siteIds}이며,
     * siteIds가 비어 있을 때만 이 값을 채운다.
     */
    private String siteId;

    @NotNull
    private UserRole userRole;

    @NotBlank
    @Size(max = 1000)
    private String reason;

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
