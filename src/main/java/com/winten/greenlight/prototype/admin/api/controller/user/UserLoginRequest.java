package com.winten.greenlight.prototype.admin.api.controller.user;

import com.winten.greenlight.prototype.admin.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginRequest {
    private String userId;
    private String password;

    public User toUser() {
        var user = new User();
        user.setUserId(userId);
        user.setPassword(password);
        return user;
    }
}