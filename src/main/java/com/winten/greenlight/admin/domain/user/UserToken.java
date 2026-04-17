package com.winten.greenlight.admin.domain.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseCookie;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserToken {
    private String accessToken;
    private String refreshToken;
    private ResponseCookie refreshCookie;
    private String tokenType;
}