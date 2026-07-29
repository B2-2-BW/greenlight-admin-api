package com.winten.greenlight.admin.domain.user;

import com.winten.greenlight.admin.db.repository.mapper.site.SiteMapper;
import com.winten.greenlight.admin.db.repository.mapper.user.UserMapper;
import com.winten.greenlight.admin.db.repository.mapper.user.UserStatusCount;
import com.winten.greenlight.admin.domain.site.SiteInfo;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserMapper userMapper;
    @Mock
    private LoginAttemptTxService loginAttemptTxService;
    @Mock
    private SiteMapper siteMapper;
    @Mock
    private CachedUserService cachedUserService;
    @Mock
    private JwtUtil jwtUtil;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void signinCreatesPendingUserForVerifiedSite() {
        PasswordManager passwordManager = new PasswordManager();
        UserService service = new UserService(
                userMapper,
                loginAttemptTxService,
                siteMapper,
                passwordManager,
                cachedUserService,
                jwtUtil
        );
        User request = User.builder()
                .userId("new-user")
                .siteId("site-a")
                .userEmail("new-user@example.com")
                .username("신규 사용자")
                .password("password123!")
                .build();

        when(siteMapper.findSiteById(any()))
                .thenReturn(Optional.of(SiteInfo.builder().siteId("site-a").build()));
        when(userMapper.findUserById("new-user"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(request));
        when(userMapper.findUserByEmail("new-user@example.com"))
                .thenReturn(Optional.empty());

        User result = service.signin(request);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userMapper).saveUser(savedUser.capture());
        assertThat(savedUser.getValue().getAccountStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(savedUser.getValue().getUserRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getValue().getPasswordResetRequired()).isFalse();
        assertThat(savedUser.getValue().getPassword()).isNull();
        assertThat(savedUser.getValue().getPasswordHash()).isNotBlank();
        assertThat(result).isSameAs(request);
    }

    @Test
    void loginRejectsActiveUserWhenSiteIsDisabled() {
        PasswordManager passwordManager = new PasswordManager();
        UserService service = createService(passwordManager);
        User user = activeUser("user", "site-a", UserRole.USER);
        user.setPasswordHash(passwordManager.encode("password123!"));

        when(userMapper.findUserLoginAttempt(any())).thenReturn(
                UserLoginAttempt.builder().loginId("user").userId("user").passwordErrorCount(0).build()
        );
        when(userMapper.findUserWithCredential("user")).thenReturn(Optional.of(user));
        when(siteMapper.findSiteById(any())).thenReturn(
                Optional.of(SiteInfo.builder().siteId("site-a").siteEnabled(false).build())
        );

        assertThatThrownBy(() -> service.login(
                LoginInfo.builder().loginId("user").password("password123!").build()
        ))
                .isInstanceOf(CoreException.class)
                .satisfies(error -> {
                    CoreException coreException = (CoreException) error;
                    assertThat(coreException.getErrorType()).isEqualTo(ErrorType.FORBIDDEN);
                    assertThat(coreException.getDetail()).isEqualTo("비활성화된 사이트의 계정은 로그인할 수 없습니다.");
                });
    }

    @Test
    void refreshRejectsActiveUserWhenSiteIsDisabled() {
        UserService service = createService(new PasswordManager());
        User user = activeUser("user", "site-a", UserRole.USER);
        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.getCurrentUserFromToken("refresh-token")).thenReturn(
                CurrentUser.builder().userId("user").userRole(UserRole.USER).userSiteId("site-a").build()
        );
        when(userMapper.findUserById("user")).thenReturn(Optional.of(user));
        when(siteMapper.findSiteById(any())).thenReturn(
                Optional.of(SiteInfo.builder().siteId("site-a").siteEnabled(false).build())
        );

        assertThatThrownBy(() -> service.refresh("refresh-token", false))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }

    @Test
    void superUserCanLoginWhenSiteIsDisabled() {
        PasswordManager passwordManager = new PasswordManager();
        UserService service = createService(passwordManager);
        User user = activeUser("super", "site-a", UserRole.SUPER);
        user.setPasswordHash(passwordManager.encode("password123!"));

        when(userMapper.findUserLoginAttempt(any())).thenReturn(
                UserLoginAttempt.builder().loginId("super").userId("super").passwordErrorCount(0).build()
        );
        when(userMapper.findUserWithCredential("super")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("token");

        assertThat(service.login(
                LoginInfo.builder().loginId("super").password("password123!").build()
        ).getAccessToken()).isEqualTo("token");
        verify(siteMapper, never()).findSiteById(any());
    }

    @Test
    void superUserCanRefreshWhenSiteIsDisabled() {
        UserService service = createService(new PasswordManager());
        User user = activeUser("super", "site-a", UserRole.SUPER);
        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.getCurrentUserFromToken("refresh-token")).thenReturn(
                CurrentUser.builder().userId("super").userRole(UserRole.SUPER).userSiteId("site-a").build()
        );
        when(userMapper.findUserById("super")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("token");

        assertThat(service.refresh("refresh-token", false).getAccessToken()).isEqualTo("token");
        verify(siteMapper, never()).findSiteById(any());
    }

    @Test
    void siteAdminListsOnlyOwnSiteUsers() {
        UserService service = new UserService(
                userMapper,
                loginAttemptTxService,
                siteMapper,
                new PasswordManager(),
                cachedUserService,
                jwtUtil
        );
        CurrentUser currentUser = CurrentUser.builder()
                .accountId(1L)
                .userId("site-admin")
                .userSiteId("site-a")
                .userRole(UserRole.SITE_ADMIN)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of())
        );
        when(userMapper.findAllUsers("site-a")).thenReturn(List.of());

        service.getManageableUsers();

        verify(userMapper).findAllUsers("site-a");
    }

    @Test
    void manageableUserPageUsesSiteScopeAndCapsOutOfRangePage() {
        UserService service = new UserService(
                userMapper, loginAttemptTxService, siteMapper, new PasswordManager(), cachedUserService, jwtUtil
        );
        CurrentUser currentUser = CurrentUser.builder()
                .accountId(1L).userId("site-admin").userSiteId("site-a").userRole(UserRole.SITE_ADMIN).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of())
        );
        UserStatusCount activeCount = new UserStatusCount();
        activeCount.setAccountStatus(AccountStatus.ACTIVE);
        activeCount.setCount(21);
        when(userMapper.countUsers("site-a", "kim")).thenReturn(21L);
        when(userMapper.countUsersByStatus("site-a", "kim")).thenReturn(List.of(activeCount));
        when(userMapper.findUsersPage("site-a", "kim", 10, 20)).thenReturn(List.of());

        UserPage result = service.getManageableUsers(99, 10, " kim ");

        assertThat(result.getPage()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getTotalElements()).isEqualTo(21);
        assertThat(result.getStatusCounts()).containsEntry(AccountStatus.ACTIVE, 21L);
        verify(userMapper).findUsersPage("site-a", "kim", 10, 20);
    }

    @Test
    void siteAdminResetUsesEnteredPasswordForUserInOwnSite() {
        PasswordManager passwordManager = new PasswordManager();
        UserService service = new UserService(
                userMapper,
                loginAttemptTxService,
                siteMapper,
                passwordManager,
                cachedUserService,
                jwtUtil
        );
        CurrentUser currentUser = CurrentUser.builder()
                .accountId(1L)
                .userId("site-admin")
                .userSiteId("site-a")
                .userRole(UserRole.SITE_ADMIN)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of())
        );
        User target = User.builder()
                .accountId(2L)
                .userId("target-user")
                .siteId("site-a")
                .userRole(UserRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(target));

        service.resetUserPassword("target-user", "AdminEntered123!");

        ArgumentCaptor<User> updatedUser = ArgumentCaptor.forClass(User.class);
        verify(userMapper).resetUserPassword(updatedUser.capture());
        assertThat(passwordManager.matches("AdminEntered123!", updatedUser.getValue().getPasswordHash())).isTrue();
        verify(loginAttemptTxService).updatePasswordErrorCountById("target-user", 0);
    }

    @Test
    void approvalUpdatesReviewedFieldsAndActivatesPendingUserAtomically() {
        UserService service = serviceWithSuperUser();
        User target = pendingUser("target-user", "site-a");
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(target));
        when(siteMapper.findSiteById(any())).thenReturn(Optional.of(SiteInfo.builder().siteId("site-b").build()));
        when(userMapper.findUserByEmail("approved@example.com")).thenReturn(Optional.empty());
        when(userMapper.approveUser(any())).thenReturn(1);

        User result = service.approveUser("target-user", "승인 사용자", "approved@example.com", "site-b", UserRole.SITE_ADMIN);

        ArgumentCaptor<User> approved = ArgumentCaptor.forClass(User.class);
        verify(userMapper).approveUser(approved.capture());
        assertThat(approved.getValue().getUsername()).isEqualTo("승인 사용자");
        assertThat(approved.getValue().getUserEmail()).isEqualTo("approved@example.com");
        assertThat(approved.getValue().getSiteId()).isEqualTo("site-b");
        assertThat(approved.getValue().getUserRole()).isEqualTo(UserRole.SITE_ADMIN);
        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void approvalRejectsNonPendingUser() {
        UserService service = serviceWithSuperUser();
        User target = pendingUser("target-user", "site-a");
        target.setAccountStatus(AccountStatus.ACTIVE);
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.approveUser("target-user", "사용자", "user@example.com", "site-a", UserRole.USER))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectedRegistrationCannotBypassApprovalThroughStatusChange() {
        UserService service = serviceWithSuperUser();
        User target = pendingUser("target-user", "site-a");
        target.setAccountStatus(AccountStatus.REJECTED);
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.updateUserStatus("target-user", AccountStatus.ACTIVE))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.INVALID_DATA);
    }

    @Test
    void siteAdminApprovalRejectsCrossSiteRequest() {
        UserService service = serviceWithSiteAdmin();
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(pendingUser("target-user", "site-a")));

        assertThatThrownBy(() -> service.approveUser("target-user", "사용자", "user@example.com", "site-b", UserRole.USER))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void siteAdminApprovalRejectsSuperRole() {
        UserService service = serviceWithSiteAdmin();
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(pendingUser("target-user", "site-a")));

        assertThatThrownBy(() -> service.approveUser("target-user", "사용자", "user@example.com", "site-a", UserRole.SUPER))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void approvalRejectsDuplicateEmail() {
        UserService service = serviceWithSuperUser();
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(pendingUser("target-user", "site-a")));
        when(siteMapper.findSiteById(any())).thenReturn(Optional.of(SiteInfo.builder().siteId("site-a").build()));
        when(userMapper.findUserByEmail("used@example.com")).thenReturn(Optional.of(User.builder().userId("other-user").build()));

        assertThatThrownBy(() -> service.approveUser("target-user", "사용자", "used@example.com", "site-a", UserRole.USER))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void activeUserManagementUpdateStoresReviewedFieldsWithoutChangingStatus() {
        UserService service = serviceWithSuperUser();
        User target = managedUser("target-user", "site-a", UserRole.USER, AccountStatus.ACTIVE);
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(target));
        when(siteMapper.findSiteById(any())).thenReturn(Optional.of(SiteInfo.builder().siteId("site-b").build()));
        when(userMapper.findUserByEmail("updated@example.com")).thenReturn(Optional.empty());
        when(userMapper.updateManagedUser(any())).thenReturn(1);

        User result = service.updateManagedUser("target-user", "수정 사용자", "updated@example.com", "site-b", UserRole.SITE_ADMIN);

        ArgumentCaptor<User> updated = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateManagedUser(updated.capture());
        assertThat(updated.getValue().getUsername()).isEqualTo("수정 사용자");
        assertThat(updated.getValue().getUserEmail()).isEqualTo("updated@example.com");
        assertThat(updated.getValue().getSiteId()).isEqualTo("site-b");
        assertThat(updated.getValue().getUserRole()).isEqualTo(UserRole.SITE_ADMIN);
        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void managementUpdateRejectsPendingAndRejectedUsers() {
        UserService service = serviceWithSuperUser();
        User pending = managedUser("pending-user", "site-a", UserRole.USER, AccountStatus.PENDING);
        User rejected = managedUser("rejected-user", "site-a", UserRole.USER, AccountStatus.REJECTED);
        when(userMapper.findUserById("pending-user")).thenReturn(Optional.of(pending));
        when(userMapper.findUserById("rejected-user")).thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> service.updateManagedUser("pending-user", "사용자", "a@example.com", "site-a", UserRole.USER))
                .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> service.updateManagedUser("rejected-user", "사용자", "a@example.com", "site-a", UserRole.USER))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void siteAdminManagementUpdateRejectsCrossSiteAndSuperRole() {
        UserService service = serviceWithSiteAdmin();
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(managedUser("target-user", "site-a", UserRole.USER, AccountStatus.ACTIVE)));

        assertThatThrownBy(() -> service.updateManagedUser("target-user", "사용자", "a@example.com", "site-b", UserRole.USER))
                .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> service.updateManagedUser("target-user", "사용자", "a@example.com", "site-a", UserRole.SUPER))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void siteAdminCanViewSameSiteSuperButCannotMutateIt() {
        UserService service = serviceWithSiteAdmin();
        User target = managedUser("site-super", "site-a", UserRole.SUPER, AccountStatus.ACTIVE);
        when(userMapper.findUserById("site-super")).thenReturn(Optional.of(target));

        assertThat(service.getViewableUser("site-super")).isSameAs(target);

        assertThatThrownBy(() -> service.updateUserStatus("site-super", AccountStatus.DISABLED))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
        assertThatThrownBy(() -> service.updateManagedUser("site-super", "수퍼", "super@example.com", "site-a", UserRole.SUPER))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
        assertThatThrownBy(() -> service.resetUserPassword("site-super", "AdminEntered123!"))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }

    @Test
    void siteAdminCannotViewOtherSiteUser() {
        UserService service = serviceWithSiteAdmin();
        when(userMapper.findUserById("other-site-user"))
                .thenReturn(Optional.of(managedUser("other-site-user", "site-b", UserRole.SUPER, AccountStatus.ACTIVE)));

        assertThatThrownBy(() -> service.getViewableUser("other-site-user"))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }

    @Test
    void managementUpdateRejectsDuplicateEmail() {
        UserService service = serviceWithSuperUser();
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(managedUser("target-user", "site-a", UserRole.USER, AccountStatus.ACTIVE)));
        when(siteMapper.findSiteById(any())).thenReturn(Optional.of(SiteInfo.builder().siteId("site-a").build()));
        when(userMapper.findUserByEmail("used@example.com")).thenReturn(Optional.of(User.builder().userId("other-user").build()));

        assertThatThrownBy(() -> service.updateManagedUser("target-user", "사용자", "used@example.com", "site-a", UserRole.USER))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void managementUpdateKeepsSelfRoleAndSiteUnchanged() {
        UserService service = serviceWithSuperUser();
        User target = managedUser("super", "site-a", UserRole.SUPER, AccountStatus.ACTIVE);
        when(userMapper.findUserById("super")).thenReturn(Optional.of(target));
        when(siteMapper.findSiteById(any())).thenReturn(Optional.of(SiteInfo.builder().siteId("site-a").build()));
        when(userMapper.findUserByEmail("self@example.com")).thenReturn(Optional.empty());
        when(userMapper.updateManagedUser(any())).thenReturn(1);

        service.updateManagedUser("super", "수정 슈퍼", "self@example.com", "site-b", UserRole.USER);

        ArgumentCaptor<User> updated = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateManagedUser(updated.capture());
        assertThat(updated.getValue().getSiteId()).isEqualTo("site-a");
        assertThat(updated.getValue().getUserRole()).isEqualTo(UserRole.SUPER);
    }

    private UserService serviceWithSuperUser() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                CurrentUser.builder().accountId(1L).userId("super").userSiteId("site-a").userRole(UserRole.SUPER).build(), null, List.of()
        ));
        return createService(new PasswordManager());
    }

    private UserService serviceWithSiteAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                CurrentUser.builder().accountId(1L).userId("site-admin").userSiteId("site-a").userRole(UserRole.SITE_ADMIN).build(), null, List.of()
        ));
        return createService(new PasswordManager());
    }

    private UserService createService(PasswordManager passwordManager) {
        return new UserService(userMapper, loginAttemptTxService, siteMapper, passwordManager, cachedUserService, jwtUtil);
    }

    private User activeUser(String userId, String siteId, UserRole userRole) {
        return User.builder()
                .accountId(2L)
                .userId(userId)
                .siteId(siteId)
                .userRole(userRole)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    private User pendingUser(String userId, String siteId) {
        return User.builder().accountId(2L).userId(userId).siteId(siteId).userRole(UserRole.USER)
                .accountStatus(AccountStatus.PENDING).build();
    }

    private User managedUser(String userId, String siteId, UserRole userRole, AccountStatus accountStatus) {
        return User.builder().accountId(2L).userId(userId).siteId(siteId).userRole(userRole)
                .accountStatus(accountStatus).build();
    }
}
