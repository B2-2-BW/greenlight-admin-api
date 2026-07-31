package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.user.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {
    @NotNull
    private AccountStatus accountStatus;

    @NotBlank
    @Size(max = 1000)
    private String reason;
}
