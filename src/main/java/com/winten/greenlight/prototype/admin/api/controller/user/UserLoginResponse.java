package com.winten.greenlight.prototype.admin.api.controller.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserLoginResponse {
    private String accessToken;
}