package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.user.AccountStatus;
import com.winten.greenlight.admin.domain.user.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long accountId;
    private String siteId;
    private String siteName;
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
