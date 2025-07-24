package com.winten.greenlight.prototype.admin.api.controller.user;

import com.winten.greenlight.prototype.admin.domain.user.User;
import lombok.Data;

@Data
public class UserResponse {
    private String username;
    private String userNickname;
}