package com.winten.greenlight.prototype.admin.api.controller.user;

import com.winten.greenlight.prototype.admin.domain.user.User;
import com.winten.greenlight.prototype.admin.domain.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCreateRequest {
    private String userId;
    private String username;
    private String password;
    private UserRole userRole;
}