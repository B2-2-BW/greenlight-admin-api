package com.winten.greenlight.prototype.admin.api.controller.user;

import com.winten.greenlight.prototype.admin.domain.user.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserConverter userConverter;

    @PostMapping("login")
    public ResponseEntity<UserLoginResponse> login(
            @RequestBody final UserLoginRequest userRequest
    ) {
        var loginResult = userService.login(userConverter.toDto(userRequest));
        return ResponseEntity.status(HttpStatus.OK)
                .body(userConverter.toResponse(loginResult));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody final UserCreateRequest userRequest,
                                                   @AuthenticationPrincipal final CurrentUser currentUser) {
        var user = userService.createUser(userConverter.toDto(userRequest), currentUser);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userConverter.toResponse(user));
    }

}