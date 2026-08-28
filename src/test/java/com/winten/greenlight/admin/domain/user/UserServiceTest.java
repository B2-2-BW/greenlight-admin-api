package com.winten.greenlight.admin.domain.user;

import com.winten.greenlight.admin.api.controller.user.UserBulkAction;
import com.winten.greenlight.admin.db.repository.mapper.site.SiteMapper;
import com.winten.greenlight.admin.db.repository.mapper.user.UserMapper;
import com.winten.greenlight.admin.db.repository.mapper.user.UserStatusCount;
import com.winten.greenlight.admin.domain.audit.AuditAction;
import com.winten.greenlight.admin.domain.audit.AuditService;
import com.winten.greenlight.admin.domain.site.SiteInfo;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
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
    @Mock
    private AuditService auditService;

    @BeforeEach
    void allowExistingSiteByDefault() {
        lenient().when(siteMapper.findSiteById(any()))
                .thenReturn(Optional.of(SiteInfo.builder().siteId("site-a").build()));
        lenient().when(userMapper.findAccessibleSitesByAccountIds(any())).thenAnswer(invocation -> {
            List<Long> accountIds = invocation.getArgument(0);
            if (accountIds == null || accountIds.isEmpty()) {
                return List.of();
            }
            return accountIds.stream()
                    .map(accountId -> UserSite.builder().accountId(accountId).siteId("site-a").build())
                    .toList();
        });
        lenient().when(userMapper.findSiteIdsByAccountId(any())).thenReturn(List.of("site-a"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void signinCreatesPendingUserWithoutSiteGrant() {
        PasswordManager passwordManager = new PasswordManager();
        UserService service = new UserService(
                userMapper,
                loginAttemptTxService,
                siteMapper,
                passwordManager,
                cachedUserService,
                jwtUtil,
                auditService
        );
        User request = User.builder()
                .userId("new-user")
                .siteId("site-a")
                .userEmail("new-user@example.com")
                .username("신규 사용자")
                .password("password123!")
                .build();

        when(userMapper.findUserById("new-user"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(request));
        when(userMapper.findUserByEmail("new-user@example.com"))
                .thenReturn(Optional.empty());

        User result = service.signin(request);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userMapper).saveUser(savedUser.capture());
        assertThat(savedUser.getValue().getSiteId()).isNull();
        assertThat(savedUser.getValue().getAccountStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(savedUser.getValue().getUserRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getValue().getPasswordResetRequired()).isFalse();
        assertThat(savedUser.getValue().getPassword()).isNull();
        assertThat(savedUser.getValue().getPasswordHash()).isNotBlank();
        assertThat(savedUser.getValue().getProfileColor()).matches("^#[0-9A-F]{6}$");
        assertThat(savedUser.getValue().getProfileInitials()).isEqualTo("신");
        assertThat(result).isSameAs(request);
        verify(userMapper, never()).insertSiteAccess(any(), any());
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
    void loginRejectsWhenPasswordResetRequiredWithoutIssuingToken() {
        PasswordManager passwordManager = new PasswordManager();
        UserService service = createService(passwordManager);
        User user = activeUser("user", "site-a", UserRole.USER);
        user.setPasswordHash(passwordManager.encode("TempPass123!"));
        user.setPasswordResetRequired(true);

        when(userMapper.findUserLoginAttempt(any())).thenReturn(
                UserLoginAttempt.builder().loginId("user").userId("user").passwordErrorCount(0).build()
        );
        when(userMapper.findUserWithCredential("user")).thenReturn(Optional.of(user));
        when(siteMapper.findSiteById(any())).thenReturn(
                Optional.of(SiteInfo.builder().siteId("site-a").siteEnabled(true).build())
        );

        assertThatThrownBy(() -> service.login(
                LoginInfo.builder().loginId("user").password("TempPass123!").build()
        ))
                .isInstanceOf(CoreException.class)
                .satisfies(error -> {
                    CoreException coreException = (CoreException) error;
                    assertThat(coreException.getErrorType()).isEqualTo(ErrorType.USER_PASSWORD_RESET_REQUIRED);
                });
        verify(jwtUtil, never()).generateToken(any(), any(), any());
        verify(loginAttemptTxService, never()).updatePasswordErrorCountById(anyString(), anyInt());
    }

    @Test
    void refreshRejectsWhenPasswordResetRequired() {
        UserService service = createService(new PasswordManager());
        User user = activeUser("user", "site-a", UserRole.USER);
        user.setPasswordResetRequired(true);
        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.getCurrentUserFromToken("refresh-token")).thenReturn(
                CurrentUser.builder().userId("user").userRole(UserRole.USER).userSiteId("site-a").build()
        );
        when(userMapper.findUserById("user")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.refresh("refresh-token", false))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.USER_PASSWORD_RESET_REQUIRED);
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void changePasswordWhenResetRequiredUpdatesPasswordWithoutToken() {
        PasswordManager passwordManager = new PasswordManager();
        UserService service = createService(passwordManager);
        User user = activeUser("user", "site-a", UserRole.USER);
        user.setPasswordHash(passwordManager.encode("TempPass123!"));
        user.setPasswordResetRequired(true);

        when(userMapper.findUserLoginAttempt(any())).thenReturn(
                UserLoginAttempt.builder().loginId("user").userId("user").passwordErrorCount(0).build()
        );
        when(userMapper.findUserWithCredential("user")).thenReturn(Optional.of(user));
        when(siteMapper.findSiteById(any())).thenReturn(
                Optional.of(SiteInfo.builder().siteId("site-a").siteEnabled(true).build())
        );

        service.changePasswordWhenResetRequired("user", "TempPass123!", "NewPass123!");

        ArgumentCaptor<User> updated = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateUserPassword(updated.capture());
        assertThat(passwordManager.matches("NewPass123!", updated.getValue().getPasswordHash())).isTrue();
        verify(loginAttemptTxService).updatePasswordErrorCountById("user", 0);
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void changePasswordWhenResetRequiredRejectsWhenNotRequired() {
        PasswordManager passwordManager = new PasswordManager();
        UserService service = createService(passwordManager);
        User user = activeUser("user", "site-a", UserRole.USER);
        user.setPasswordHash(passwordManager.encode("TempPass123!"));
        user.setPasswordResetRequired(false);

        when(userMapper.findUserLoginAttempt(any())).thenReturn(
                UserLoginAttempt.builder().loginId("user").userId("user").passwordErrorCount(0).build()
        );
        when(userMapper.findUserWithCredential("user")).thenReturn(Optional.of(user));
        when(siteMapper.findSiteById(any())).thenReturn(
                Optional.of(SiteInfo.builder().siteId("site-a").siteEnabled(true).build())
        );

        assertThatThrownBy(() -> service.changePasswordWhenResetRequired("user", "TempPass123!", "NewPass123!"))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.INVALID_DATA);
        verify(userMapper, never()).updateUserPassword(any());
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
                jwtUtil,
                auditService
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
                userMapper, loginAttemptTxService, siteMapper, new PasswordManager(), cachedUserService, jwtUtil,
                auditService
        );
        CurrentUser currentUser = CurrentUser.builder()
                .accountId(1L).userId("site-admin").userSiteId("site-a").userRole(UserRole.SITE_ADMIN).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of())
        );
        UserStatusCount activeCount = new UserStatusCount();
        activeCount.setAccountStatus(AccountStatus.ACTIVE);
        activeCount.setCount(21);
        when(userMapper.countUsers("site-a", "kim", null, null)).thenReturn(21L);
        when(userMapper.countUsersByStatus("site-a", "kim", null)).thenReturn(List.of(activeCount));
        when(userMapper.findUsersPage("site-a", "kim", null, null, 10, 20)).thenReturn(List.of());

        UserPage result = service.getManageableUsers(99, 10, " kim ", null, null, null);

        assertThat(result.getPage()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getTotalElements()).isEqualTo(21);
        assertThat(result.getStatusCounts()).containsEntry(AccountStatus.ACTIVE, 21L);
        verify(userMapper).findUsersPage("site-a", "kim", null, null, 10, 20);
    }

    @Test
    void superUsesCurrentUserSiteAndIgnoresLegacyRequestedSiteFilter() {
        UserService service = serviceWithSuperUser();
        when(userMapper.countUsers("site-a", null, AccountStatus.PENDING, UserRole.USER)).thenReturn(1L);
        when(userMapper.countUsersByStatus("site-a", null, UserRole.USER)).thenReturn(List.of());
        when(userMapper.findUsersPage(
                "site-a", null, AccountStatus.PENDING, UserRole.USER, 10, 0
        )).thenReturn(List.of());

        service.getManageableUsers(
                1, 10, null, AccountStatus.PENDING, UserRole.USER, " site-b "
        );

        verify(userMapper).countUsers("site-a", null, AccountStatus.PENDING, UserRole.USER);
        verify(userMapper).countUsersByStatus("site-a", null, UserRole.USER);
    }

    @Test
    void siteAdminIgnoresRequestedSiteFilterAndUsesOwnSite() {
        UserService service = serviceWithSiteAdmin();
        when(userMapper.countUsers("site-a", null, null, null)).thenReturn(0L);
        when(userMapper.countUsersByStatus("site-a", null, null)).thenReturn(List.of());

        service.getManageableUsers(1, 10, null, null, null, "site-b");

        verify(userMapper).countUsers("site-a", null, null, null);
        verify(userMapper).countUsersByStatus("site-a", null, null);
    }

    @Test
    void bulkRejectReusesSingleStatusValidationAndRecordsAudit() {
        UserService service = serviceWithSuperUser();
        User pending = pendingUser("pending-user", "site-a");
        pending.setUsername("대기 사용자");
        pending.setUserEmail("pending@example.com");
        when(userMapper.findUserById("pending-user")).thenReturn(Optional.of(pending));

        int updatedCount = service.bulkAction(
                List.of("pending-user"), UserBulkAction.REJECT, "가입 정보 불충분"
        );

        assertThat(updatedCount).isEqualTo(1);
        assertThat(pending.getAccountStatus()).isEqualTo(AccountStatus.REJECTED);
        verify(userMapper).updateUserStatus(pending);
        verify(auditService).recordChanges(
                "site-a",
                "USER",
                "pending-user",
                AuditAction.UPDATE,
                "가입 정보 불충분",
                Map.of("accountStatus", "PENDING"),
                Map.of("accountStatus", "REJECTED"),
                List.of("accountStatus")
        );
    }

    @Test
    void bulkApproveUsesPendingUsersExistingProfile() {
        UserService service = serviceWithSuperUser();
        User pending = pendingUser("pending-user", "site-a");
        pending.setUsername("대기 사용자");
        pending.setUserEmail("pending@example.com");
        when(userMapper.findUserById("pending-user")).thenReturn(Optional.of(pending));
        when(siteMapper.findSiteById(any())).thenReturn(Optional.of(SiteInfo.builder().siteId("site-a").build()));
        when(userMapper.findUserByEmail("pending@example.com")).thenReturn(Optional.empty());
        when(userMapper.approveUser(any())).thenReturn(1);

        service.bulkAction(List.of("pending-user"), UserBulkAction.APPROVE, "가입 승인");

        ArgumentCaptor<User> approved = ArgumentCaptor.forClass(User.class);
        verify(userMapper).approveUser(approved.capture());
        assertThat(approved.getValue().getUsername()).isEqualTo("대기 사용자");
        assertThat(approved.getValue().getUserEmail()).isEqualTo("pending@example.com");
        assertThat(approved.getValue().getSiteId()).isEqualTo("site-a");
        assertThat(approved.getValue().getUserRole()).isEqualTo(UserRole.USER);
        verify(auditService).recordChanges(
                "site-a",
                "USER",
                "pending-user",
                AuditAction.UPDATE,
                "가입 승인",
                Map.of("accountStatus", "PENDING"),
                Map.of("accountStatus", "ACTIVE"),
                List.of("accountStatus")
        );
    }

    @Test
    void bulkDisableRejectsNonActiveUserBeforeMutation() {
        UserService service = serviceWithSuperUser();
        User pending = pendingUser("pending-user", "site-a");
        when(userMapper.findUserById("pending-user")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.bulkAction(
                List.of("pending-user"), UserBulkAction.DISABLE, "비활성화"
        )).isInstanceOf(CoreException.class);

        verify(userMapper, never()).updateUserStatus(any());
        verify(auditService, never()).recordChanges(
                any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void bulkDisableRejectsSuperUserBeforeMutation() {
        UserService service = serviceWithSuperUser();
        User target = activeUser("other-super", "site-a", UserRole.SUPER);
        when(userMapper.findUserById("other-super")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.bulkAction(
                List.of("other-super"), UserBulkAction.DISABLE, "권한 정리"
        ))
                .isInstanceOf(CoreException.class)
                .satisfies(error ->
                        assertThat(((CoreException) error).getDetail())
                                .isEqualTo("SUPER 계정은 비활성화할 수 없습니다."));

        verify(userMapper, never()).updateUserStatus(any());
        verify(auditService, never()).recordChanges(
                any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void directStatusUpdateCannotDisableSuperUser() {
        UserService service = serviceWithSuperUser();
        User target = activeUser("other-super", "site-a", UserRole.SUPER);
        when(userMapper.findUserById("other-super")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.updateUserStatus("other-super", AccountStatus.DISABLED))
                .isInstanceOf(CoreException.class)
                .satisfies(error ->
                        assertThat(((CoreException) error).getDetail())
                                .isEqualTo("SUPER 계정은 비활성화할 수 없습니다."));

        verify(userMapper, never()).updateUserStatus(any());
    }

    @Test
    void directStatusUpdateRecordsReasonAndStatusDiff() {
        UserService service = serviceWithSuperUser();
        User target = managedUser("rejected-user", "site-a", UserRole.USER, AccountStatus.REJECTED);
        when(userMapper.findUserById("rejected-user")).thenReturn(Optional.of(target));

        service.updateUserStatus("rejected-user", AccountStatus.PENDING, "가입 정보 재검토");

        verify(userMapper).updateUserStatus(target);
        verify(auditService).recordChanges(
                "site-a",
                "USER",
                "rejected-user",
                AuditAction.UPDATE,
                "가입 정보 재검토",
                Map.of("accountStatus", "REJECTED"),
                Map.of("accountStatus", "PENDING"),
                List.of("accountStatus")
        );
    }

    @Test
    void bulkActionRejectsUserFromDeletedSite() {
        UserService service = serviceWithSuperUser();
        User active = activeUser("deleted-site-user", "site-deleted", UserRole.USER);
        when(userMapper.findUserById("deleted-site-user")).thenReturn(Optional.of(active));
        when(siteMapper.findSiteById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.bulkAction(
                List.of("deleted-site-user"), UserBulkAction.DISABLE, "퇴사"
        ))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.SITE_NOT_FOUND);

        verify(userMapper, never()).updateUserStatus(any());
        verify(auditService, never()).recordChanges(
                any(), any(), any(), any(), any(), any(), any(), any()
        );
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
                jwtUtil,
                auditService
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
        verify(auditService).recordChanges(
                "site-a",
                "USER",
                "target-user",
                AuditAction.UPDATE,
                null,
                Map.of("passwordReset", false),
                Map.of("passwordReset", true),
                List.of("passwordReset")
        );
    }

    @Test
    void approvalUpdatesReviewedFieldsAndActivatesPendingUserAtomically() {
        UserService service = serviceWithSuperUser();
        User target = pendingUser("target-user", "site-a");
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(target));
        when(siteMapper.findSiteById(any())).thenReturn(
                Optional.of(SiteInfo.builder().siteId("site-b").siteEnabled(true).build())
        );
        when(userMapper.findUserByEmail("approved@example.com")).thenReturn(Optional.empty());
        when(userMapper.approveUser(any())).thenReturn(1);

        User result = service.approveUser("target-user", "승인 사용자", "approved@example.com", List.of("site-b"), UserRole.SITE_ADMIN);

        ArgumentCaptor<User> approved = ArgumentCaptor.forClass(User.class);
        verify(userMapper).approveUser(approved.capture());
        assertThat(approved.getValue().getUsername()).isEqualTo("승인 사용자");
        assertThat(approved.getValue().getUserEmail()).isEqualTo("approved@example.com");
        assertThat(approved.getValue().getSiteId()).isEqualTo("site-b");
        assertThat(approved.getValue().getUserRole()).isEqualTo(UserRole.SITE_ADMIN);
        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void detailedApprovalRecordsReasonAndReviewedFieldChangesOnce() {
        UserService service = serviceWithSuperUser();
        User target = pendingUser("target-user", "site-a");
        target.setUsername("가입 사용자");
        target.setUserEmail("pending@example.com");
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(target));
        when(userMapper.findAccessibleSitesByAccountIds(any()))
                .thenReturn(List.of(UserSite.builder().accountId(2L).siteId("site-a").build()))
                .thenReturn(List.of(UserSite.builder().accountId(2L).siteId("site-b").build()));
        when(siteMapper.findSiteById(any())).thenReturn(
                Optional.of(SiteInfo.builder().siteId("site-b").siteEnabled(true).build())
        );
        when(userMapper.findUserByEmail("approved@example.com")).thenReturn(Optional.empty());
        when(userMapper.approveUser(any())).thenReturn(1);

        service.approveUser(
                "target-user",
                "승인 사용자",
                "approved@example.com",
                List.of("site-b"),
                UserRole.SITE_ADMIN,
                "담당자 검토 완료"
        );

        verify(auditService).recordChanges(
                "site-b",
                "USER",
                "target-user",
                AuditAction.UPDATE,
                "담당자 검토 완료",
                Map.of(
                        "accountStatus", "PENDING",
                        "username", "가입 사용자",
                        "userEmail", "pending@example.com",
                        "siteIds", List.of("site-a"),
                        "userRole", "USER"
                ),
                Map.of(
                        "accountStatus", "ACTIVE",
                        "username", "승인 사용자",
                        "userEmail", "approved@example.com",
                        "siteIds", List.of("site-b"),
                        "userRole", "SITE_ADMIN"
                ),
                List.of("accountStatus", "username", "userEmail", "siteIds", "userRole")
        );
    }

    @Test
    void approvalRejectsNonPendingUser() {
        UserService service = serviceWithSuperUser();
        User target = pendingUser("target-user", "site-a");
        target.setAccountStatus(AccountStatus.ACTIVE);
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.approveUser("target-user", "사용자", "user@example.com", List.of("site-a"), UserRole.USER))
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

        assertThatThrownBy(() -> service.approveUser("target-user", "사용자", "user@example.com", List.of("site-b"), UserRole.USER))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void siteAdminApprovalRejectsSuperRole() {
        UserService service = serviceWithSiteAdmin();
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(pendingUser("target-user", "site-a")));

        assertThatThrownBy(() -> service.approveUser("target-user", "사용자", "user@example.com", List.of("site-a"), UserRole.SUPER))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void approvalUsesGrantedSiteWhenAccountSiteIdIsBlank() {
        UserService service = serviceWithSuperUser();
        User target = pendingUser("target-user", " ");
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(target));
        when(siteMapper.findSiteById(any())).thenReturn(
                Optional.of(SiteInfo.builder().siteId("site-b").siteEnabled(true).build())
        );
        when(userMapper.findUserByEmail("approved@example.com")).thenReturn(Optional.empty());
        when(userMapper.approveUser(any())).thenReturn(1);

        service.approveUser("target-user", "승인 사용자", "approved@example.com", List.of("site-b"), UserRole.USER);

        ArgumentCaptor<User> approved = ArgumentCaptor.forClass(User.class);
        verify(userMapper).approveUser(approved.capture());
        assertThat(approved.getValue().getSiteId()).isEqualTo("site-b");
        verify(userMapper).insertSiteAccessBatch(2L, List.of("site-b"));
    }

    @Test
    void approvalRejectsDuplicateEmail() {
        UserService service = serviceWithSuperUser();
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(pendingUser("target-user", "site-a")));
        when(siteMapper.findSiteById(any())).thenReturn(
                Optional.of(SiteInfo.builder().siteId("site-a").siteEnabled(true).build())
        );
        when(userMapper.findUserByEmail("used@example.com")).thenReturn(Optional.of(User.builder().userId("other-user").build()));

        assertThatThrownBy(() -> service.approveUser("target-user", "사용자", "used@example.com", List.of("site-a"), UserRole.USER))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void activeUserManagementUpdateStoresReviewedFieldsWithoutChangingStatus() {
        UserService service = serviceWithSuperUser();
        User target = managedUser("target-user", "site-a", UserRole.USER, AccountStatus.ACTIVE);
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(target));
        when(siteMapper.findSiteById(any())).thenReturn(
                Optional.of(SiteInfo.builder().siteId("site-b").siteEnabled(true).build())
        );
        when(userMapper.findUserByEmail("updated@example.com")).thenReturn(Optional.empty());
        when(userMapper.updateManagedUser(any())).thenReturn(1);

        User result = service.updateManagedUser("target-user", "수정 사용자", "updated@example.com", List.of("site-b"), UserRole.SITE_ADMIN);

        ArgumentCaptor<User> updated = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateManagedUser(updated.capture());
        assertThat(updated.getValue().getUsername()).isEqualTo("수정 사용자");
        assertThat(updated.getValue().getUserEmail()).isEqualTo("updated@example.com");
        assertThat(updated.getValue().getSiteId()).isEqualTo("site-b");
        assertThat(updated.getValue().getUserRole()).isEqualTo(UserRole.SITE_ADMIN);
        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(auditService).recordChanges(
                org.mockito.ArgumentMatchers.eq("site-b"),
                org.mockito.ArgumentMatchers.eq("USER"),
                org.mockito.ArgumentMatchers.eq("target-user"),
                org.mockito.ArgumentMatchers.eq(AuditAction.UPDATE),
                org.mockito.ArgumentMatchers.isNull(),
                anyMap(),
                anyMap(),
                org.mockito.ArgumentMatchers.eq(List.of("username", "userEmail", "siteIds", "userRole"))
        );
    }

    @Test
    void managementUpdateRejectsPendingAndRejectedUsers() {
        UserService service = serviceWithSuperUser();
        User pending = managedUser("pending-user", "site-a", UserRole.USER, AccountStatus.PENDING);
        User rejected = managedUser("rejected-user", "site-a", UserRole.USER, AccountStatus.REJECTED);
        when(userMapper.findUserById("pending-user")).thenReturn(Optional.of(pending));
        when(userMapper.findUserById("rejected-user")).thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> service.updateManagedUser("pending-user", "사용자", "a@example.com", List.of("site-a"), UserRole.USER))
                .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> service.updateManagedUser("rejected-user", "사용자", "a@example.com", List.of("site-a"), UserRole.USER))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void siteAdminManagementUpdateRejectsCrossSiteAndSuperRole() {
        UserService service = serviceWithSiteAdmin();
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(managedUser("target-user", "site-a", UserRole.USER, AccountStatus.ACTIVE)));

        assertThatThrownBy(() -> service.updateManagedUser("target-user", "사용자", "a@example.com", List.of("site-b"), UserRole.USER))
                .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> service.updateManagedUser("target-user", "사용자", "a@example.com", List.of("site-a"), UserRole.SUPER))
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
        assertThatThrownBy(() -> service.updateManagedUser("site-super", "수퍼", "super@example.com", List.of("site-a"), UserRole.SUPER))
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
        when(userMapper.findAccessibleSitesByAccountIds(any())).thenReturn(List.of(
                UserSite.builder().accountId(2L).siteId("site-b").build()
        ));

        assertThatThrownBy(() -> service.getViewableUser("other-site-user"))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }

    @Test
    void managementUpdateRejectsDuplicateEmail() {
        UserService service = serviceWithSuperUser();
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(managedUser("target-user", "site-a", UserRole.USER, AccountStatus.ACTIVE)));
        when(siteMapper.findSiteById(any())).thenReturn(
                Optional.of(SiteInfo.builder().siteId("site-a").siteEnabled(true).build())
        );
        when(userMapper.findUserByEmail("used@example.com")).thenReturn(Optional.of(User.builder().userId("other-user").build()));

        assertThatThrownBy(() -> service.updateManagedUser("target-user", "사용자", "used@example.com", List.of("site-a"), UserRole.USER))
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

        service.updateManagedUser("super", "수정 슈퍼", "self@example.com", List.of("site-b"), UserRole.USER);

        ArgumentCaptor<User> updated = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateManagedUser(updated.capture());
        assertThat(updated.getValue().getSiteId()).isEqualTo("site-a");
        assertThat(updated.getValue().getUserRole()).isEqualTo(UserRole.SUPER);
    }

    @Test
    void regularUserCanStillUpdateOwnProfile() {
        UserService service = createService(new PasswordManager());
        var currentUser = CurrentUser.builder()
                .accountId(2L)
                .userId("regular-user")
                .userSiteId("site-a")
                .userRole(UserRole.USER)
                .build();
        var user = managedUser("regular-user", "site-a", UserRole.USER, AccountStatus.ACTIVE);
        when(userMapper.findUserById("regular-user")).thenReturn(Optional.of(user));
        when(userMapper.findUserByEmail("updated@example.com")).thenReturn(Optional.empty());

        service.updateMyProfile(
                currentUser,
                "수정 사용자",
                "updated@example.com",
                "010-1234-5678",
                "#7c3aed",
                "수정"
        );

        var updated = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateUserProfile(updated.capture());
        assertThat(updated.getValue().getUsername()).isEqualTo("수정 사용자");
        assertThat(updated.getValue().getUserEmail()).isEqualTo("updated@example.com");
        assertThat(updated.getValue().getPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(updated.getValue().getProfileColor()).isEqualTo("#7C3AED");
        assertThat(updated.getValue().getProfileInitials()).isEqualTo("수정");
    }

    @Test
    void loginAllowsUserWhenAnotherGrantedSiteIsEnabled() {
        PasswordManager passwordManager = new PasswordManager();
        UserService service = createService(passwordManager);
        User user = activeUser("user", "site-a", UserRole.USER);
        user.setPasswordHash(passwordManager.encode("password123!"));

        when(userMapper.findUserLoginAttempt(any())).thenReturn(
                UserLoginAttempt.builder().loginId("user").userId("user").passwordErrorCount(0).build()
        );
        when(userMapper.findUserWithCredential("user")).thenReturn(Optional.of(user));
        when(userMapper.findSiteIdsByAccountId(2L)).thenReturn(List.of("site-a", "site-b"));
        when(siteMapper.findSiteById(any())).thenAnswer(invocation -> {
            SiteInfo param = invocation.getArgument(0);
            boolean enabled = "site-b".equals(param.getSiteId());
            return Optional.of(SiteInfo.builder().siteId(param.getSiteId()).siteEnabled(enabled).build());
        });
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("token");

        assertThat(service.login(
                LoginInfo.builder().loginId("user").password("password123!").build()
        ).getAccessToken()).isEqualTo("token");
    }

    @Test
    void siteAdminPartialSiteGrantKeepsHiddenSites() {
        UserService service = serviceWithSiteAdmin(List.of("site-a", "site-b"));
        User target = managedUser("target-user", "site-a", UserRole.USER, AccountStatus.ACTIVE);
        when(userMapper.findUserById("target-user")).thenReturn(Optional.of(target));
        when(userMapper.findAccessibleSitesByAccountIds(List.of(2L))).thenReturn(List.of(
                UserSite.builder().accountId(2L).siteId("site-a").build(),
                UserSite.builder().accountId(2L).siteId("site-c").build()
        ));
        when(siteMapper.findSiteById(any())).thenAnswer(invocation -> {
            SiteInfo param = invocation.getArgument(0);
            return Optional.of(SiteInfo.builder().siteId(param.getSiteId()).siteEnabled(true).build());
        });
        when(userMapper.findUserByEmail("updated@example.com")).thenReturn(Optional.empty());
        when(userMapper.updateManagedUser(any())).thenReturn(1);

        service.updateManagedUser(
                "target-user",
                "수정 사용자",
                "updated@example.com",
                List.of("site-b"),
                UserRole.USER
        );

        ArgumentCaptor<User> updated = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateManagedUser(updated.capture());
        assertThat(updated.getValue().getSiteId()).isEqualTo("site-b");
        verify(userMapper).deleteSiteAccessByAccountId(2L);
        verify(userMapper).insertSiteAccessBatch(2L, List.of("site-b", "site-c"));
    }

    @Test
    void siteAdminCannotGrantSiteOutsideOwnAccess() {
        UserService service = serviceWithSiteAdmin();
        when(userMapper.findUserById("target-user")).thenReturn(
                Optional.of(managedUser("target-user", "site-a", UserRole.USER, AccountStatus.ACTIVE))
        );

        assertThatThrownBy(() -> service.updateManagedUser(
                "target-user",
                "사용자",
                "a@example.com",
                List.of("site-a", "site-x"),
                UserRole.USER
        ))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
        verify(userMapper, never()).updateManagedUser(any());
    }

    private UserService serviceWithSuperUser() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                CurrentUser.builder().accountId(1L).userId("super").userSiteId("site-a").userRole(UserRole.SUPER).build(), null, List.of()
        ));
        return createService(new PasswordManager());
    }

    private UserService serviceWithSiteAdmin() {
        return serviceWithSiteAdmin(List.of("site-a"));
    }

    private UserService serviceWithSiteAdmin(List<String> accessibleSiteIds) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                CurrentUser.builder().accountId(1L).userId("site-admin").userSiteId("site-a").userRole(UserRole.SITE_ADMIN)
                        .accessibleSiteIds(accessibleSiteIds).build(), null, List.of()
        ));
        return createService(new PasswordManager());
    }

    private UserService createService(PasswordManager passwordManager) {
        return new UserService(
                userMapper, loginAttemptTxService, siteMapper, passwordManager, cachedUserService, jwtUtil, auditService
        );
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
