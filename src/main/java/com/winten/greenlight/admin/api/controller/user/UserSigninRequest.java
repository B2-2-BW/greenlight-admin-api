package com.winten.greenlight.admin.api.controller.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSigninRequest {
    @NotBlank
    private String userId;
    @NotBlank
    @Email
    private String userEmail;
    @NotBlank
    private String username;
    @NotBlank
    @Size(min = 8, max = 100)
    private String password;
    private String phoneNumber;
}
