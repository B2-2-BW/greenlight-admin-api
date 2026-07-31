package com.winten.greenlight.admin.api.controller.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    private String userEmail;

    @Size(max = 30)
    private String phoneNumber;

    @NotBlank
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    private String profileColor;

    @NotBlank
    @Size(max = 2)
    private String profileInitials;
}
