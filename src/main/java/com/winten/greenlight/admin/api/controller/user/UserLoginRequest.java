package com.winten.greenlight.admin.api.controller.user;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginRequest {
    @JsonAlias("userId")
    @NotBlank
    private String loginId;
    @NotBlank
    private String password;
}
