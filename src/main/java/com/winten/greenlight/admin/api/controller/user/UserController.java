package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.AccountStatus;
import com.winten.greenlight.admin.domain.user.UserConverter;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.domain.user.UserService;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserService userService;
    private final UserConverter userConverter;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        var user = userService.me(currentUser);
        return ResponseEntity.ok(userConverter.toResponse(user));
    }

    @GetMapping
    public ResponseEntity<UserPageResponse> getUsers(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String siteId
    ) {
        var result = userService.getManageableUsers(page, size, query, status, role, siteId);
        var users = result.getContent().stream()
                .map(userConverter::toResponse)
                .toList();
        return ResponseEntity.ok(UserPageResponse.builder()
                .content(users)
                .page(result.getPage())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .statusCounts(result.getStatusCounts())
                .build());
    }

    @PostMapping("/bulk-actions")
    public ResponseEntity<UserBulkActionResponse> bulkAction(
            @RequestBody @Valid UserBulkActionRequest request
    ) {
        int updatedCount = userService.bulkAction(
                request.getUserIds(),
                request.getAction(),
                request.getReason()
        );
        return ResponseEntity.ok(new UserBulkActionResponse(updatedCount));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        var user = userService.getViewableUser(userId);
        return ResponseEntity.ok(userConverter.toResponse(user));
    }

    @PostMapping("login")
    public ResponseEntity<UserLoginResponse> login(
            @RequestBody @Valid final UserLoginRequest userRequest
    ) {
        var loginResult = userService.login(userConverter.toDto(userRequest));
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, loginResult.getRefreshCookie().toString())
                .body(userConverter.toResponse(loginResult));
    }

    /**
     * 관리자 비밀번호 초기화 후, 로그인 전에 새 비밀번호로 변경한다.
     * 인증 토큰을 발급하지 않으며, 기존 refresh 쿠키가 있으면 삭제한다.
     */
    @PostMapping("password/change-required")
    public ResponseEntity<Void> changePasswordWhenResetRequired(
            @RequestBody @Valid final UserPasswordChangeRequiredRequest request
    ) {
        userService.changePasswordWhenResetRequired(
                request.getLoginId(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        var deleteRefreshTokenCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .sameSite("lax")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteRefreshTokenCookie.toString())
                .build();
    }

    @PostMapping("refresh")
    public ResponseEntity<UserLoginResponse> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @RequestParam(required = false) boolean autoLogin
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "Refresh Token is missing");
        }

        var refreshResult = userService.refresh(refreshToken, autoLogin);

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, refreshResult.getRefreshCookie().toString())
                .body(userConverter.toResponse(refreshResult));
    }

    @PostMapping("logout")
    public ResponseEntity<UserLogoutResponse> logout() {
        var deleteRefreshTokenCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .sameSite("lax")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, deleteRefreshTokenCookie.toString())
                .body(new UserLogoutResponse(true));
    }

    // 회원가입
    @PostMapping("signin")
    public ResponseEntity<UserResponse> signin(
            @RequestBody @Valid final UserSigninRequest userRequest
    ) {
        var signinResult = userService.signin(userConverter.toDto(userRequest));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userConverter.toResponse(signinResult));
    }

    @PutMapping("{userId}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable String userId,
            @RequestBody @Valid final UserUpdateRequest userRequest
    ) {
        var user = userService.updateUserStatus(
                userId,
                userRequest.getAccountStatus(),
                userRequest.getReason()
        );
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userConverter.toResponse(user));
    }

    @PutMapping("{userId}/approval")
    public ResponseEntity<UserResponse> approveUser(
            @PathVariable String userId,
            @RequestBody @Valid UserApprovalRequest request
    ) {
        var user = userService.approveUser(
                userId,
                request.getUsername(),
                request.getUserEmail(),
                request.resolveSiteIds(),
                request.getUserRole(),
                request.getReason()
        );
        return ResponseEntity.ok(userConverter.toResponse(user));
    }

    @PutMapping("{userId}")
    public ResponseEntity<UserResponse> updateManagedUser(
            @PathVariable String userId,
            @RequestBody @Valid UserManagementUpdateRequest request
    ) {
        var user = userService.updateManagedUser(
                userId,
                request.getUsername(),
                request.getUserEmail(),
                request.resolveSiteIds(),
                request.getUserRole()
        );
        return ResponseEntity.ok(userConverter.toResponse(user));
    }

    @PostMapping("{userId}/password/reset")
    public ResponseEntity<UserResponse> resetUserPassword(
            @PathVariable String userId,
            @RequestBody @Valid UserPasswordResetRequest request
    ) {
        var user = userService.resetUserPassword(userId, request.getNewPassword());
        return ResponseEntity.ok(userConverter.toResponse(user));
    }

    @PutMapping("me/password")
    public ResponseEntity<UserResponse> changeMyPassword(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestBody @Valid UserPasswordChangeRequest request
    ) {
        var user = userService.changeMyPassword(
                currentUser,
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(userConverter.toResponse(user));
    }

    @PutMapping("me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestBody @Valid UserProfileUpdateRequest request
    ) {
        var user = userService.updateMyProfile(
                currentUser,
                request.getUsername(),
                request.getUserEmail(),
                request.getPhoneNumber(),
                request.getProfileColor(),
                request.getProfileInitials()
        );
        return ResponseEntity.ok(userConverter.toResponse(user));
    }

}
