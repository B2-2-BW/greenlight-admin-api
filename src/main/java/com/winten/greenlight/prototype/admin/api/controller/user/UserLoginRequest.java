package com.winten.greenlight.prototype.admin.api.controller.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginRequest {
    private String userId;
    private String password;
}