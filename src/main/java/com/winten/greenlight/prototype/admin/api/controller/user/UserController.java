package com.winten.greenlight.prototype.admin.api.controller.user;

import com.winten.greenlight.prototype.admin.domain.user.CurrentUser;
import com.winten.greenlight.prototype.admin.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("login")
    public ResponseEntity<UserLoginResponse> login(@RequestBody final UserLoginRequest user) {
        var token = userService.login(user.toUser());
        var response = new UserLoginResponse(token.getAccessToken());
        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody final UserCreateRequest userCreateRequest,
                                                   @AuthenticationPrincipal final CurrentUser currentUser) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new UserResponse(userService.createUser(userCreateRequest.toUser(), currentUser)));
    }


}