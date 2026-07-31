package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserApprovalRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String userEmail;

    @NotBlank
    private String siteId;

    @NotNull
    private UserRole userRole;

    @NotBlank
    @Size(max = 1000)
    private String reason;
}
