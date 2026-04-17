package com.winten.greenlight.admin.domain.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class LoginInfo {
    private String loginId;
    private String password;
    private boolean autoLogin;
}