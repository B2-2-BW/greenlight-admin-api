package com.winten.greenlight.admin.api.controller.user;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginRequest {
    @JsonAlias("userId")
    private String loginId;
    private String password;
    private boolean autoLogin;
}