package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.user.AccountStatus;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.domain.user.UserSite;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserResponse {
    private Long accountId;
    private String siteId;
    private String siteName;
    private List<String> siteIds;
    private List<UserSite> sites;
    private String username;
    private String profileColor;
    private String profileInitials;
    private String userId;
    private String userEmail;
    private String phoneNumber;
    private UserRole userRole;
    private AccountStatus accountStatus;
    private Boolean passwordResetRequired;
    private LocalDateTime createdAt;
}
