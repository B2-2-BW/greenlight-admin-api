package com.winten.greenlight.admin.api.controller.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSigninRequest {
    private String userId;
    private String siteId;
    private String userEmail;
    private String username;
    private String password;
    private String phoneNumber;
}