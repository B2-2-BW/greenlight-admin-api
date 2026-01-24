package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserConverter;
import com.winten.greenlight.admin.domain.user.UserService;
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

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        var user = userService.me(currentUser);
        return ResponseEntity.ok(userConverter.toResponse(user));
    }

    @PostMapping("login")
    public ResponseEntity<UserLoginResponse> login(
            @RequestBody final UserLoginRequest userRequest
    ) {
        var loginResult = userService.login(userConverter.toDto(userRequest));
        return ResponseEntity.status(HttpStatus.OK)
                .body(userConverter.toResponse(loginResult));
    }

    // 회원가입
    @PostMapping("signin")
    public ResponseEntity<UserResponse> signin(
            @RequestBody final UserSigninRequest userRequest
    ) {
        var signinResult = userService.signin(userConverter.toDto(userRequest));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userConverter.toResponse(signinResult));
    }

    @PutMapping("{userId}/status")
    public ResponseEntity<UserResponse> updateUserStatus(@RequestBody final UserUpdateRequest userRequest) {
        var user = userService.updateUserStatus(userConverter.toDto(userRequest));
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userConverter.toResponse(user));
    }

}