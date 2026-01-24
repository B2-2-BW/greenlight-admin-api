package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.user.AccountStatus;
import com.winten.greenlight.admin.domain.user.UserRole;
import lombok.Data;

@Data
public class UserResponse {
    private Long accountId;
    private String siteId;
    private String siteName;
    private String username;
    private String userId;
    private UserRole userRole;
    private AccountStatus accountStatus;
}