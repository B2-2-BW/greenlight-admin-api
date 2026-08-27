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

    @NotNull
    private UserRole userRole;
}
