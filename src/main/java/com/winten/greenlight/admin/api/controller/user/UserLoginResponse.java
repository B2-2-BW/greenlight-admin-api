package com.winten.greenlight.admin.api.controller.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;

@Data
@AllArgsConstructor
public class UserLoginResponse {
    private String accessToken;
    private String tokenType;
    private Instant refreshExpiresAt;
}
