package com.winten.greenlight.prototype.admin.api.controller.user;

import com.winten.greenlight.prototype.admin.domain.user.User;
import lombok.Data;

@Data
public class UserRequestDto {
    private String username;
    private String userNickname;
    private String password;
    private String passcode;

    public User toUser() {
        var user = new User();
        user.setUsername(username);
        user.setUserNickname(userNickname);
        user.setPassword(password);
        user.setPasscode(passcode);
        return user;
    }
}