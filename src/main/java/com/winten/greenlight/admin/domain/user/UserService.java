package com.winten.greenlight.admin.domain.user;

import com.winten.greenlight.admin.db.repository.mapper.site.SiteMapper;
import com.winten.greenlight.admin.db.repository.mapper.user.UserMapper;
import com.winten.greenlight.admin.db.repository.mapper.user.UserStatusCount;
import com.winten.greenlight.admin.api.controller.user.UserBulkAction;
import com.winten.greenlight.admin.domain.audit.AuditAction;
import com.winten.greenlight.admin.domain.audit.AuditService;
import com.winten.greenlight.admin.domain.site.SiteInfo;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import com.winten.greenlight.admin.support.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final List<String> PROFILE_COLOR_PRESETS = List.of(
            "#2563EB",
            "#7C3AED",
            "#DB2777",
            "#DC2626",
            "#EA580C",
            "#16A34A",
            "#0891B2",
            "#475569"
    );
    private static final Pattern PROFILE_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final String ANONYMOUS_ACTOR = "ANONYMOUS";
    private static final List<String> MANAGED_USER_AUDIT_FIELDS = List.of(
            "username", "userEmail", "siteIds", "userRole"
    );

    private final UserMapper userMapper;
    private final LoginAttemptTxService loginAttemptTxService;
    private final SiteMapper siteMapper;
    private final PasswordManager passwordManager;
    private final CachedUserService cachedUserService;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    @Value("${jwt.expiration.access:1800}") // 1시간
    private Long accessTokenExpiration;

    @Value("${jwt.expiration.refresh:1209600}") // 14일
    private Long refreshTokenExpiration;

    @Transactional(readOnly = true)
    public User me(CurrentUser currentUser) {
        return this.findUserById(currentUser.getUserId());
    }

    @Transactional(readOnly = true)
    public UserToken login(LoginInfo loginParam) {
        // 사용자 로그인 시도 조회
        var userLoginAttempt = userMapper.findUserLoginAttempt(
                UserLoginAttempt.builder()
                        .loginId(loginParam.getLoginId())
                        .build()
        );

        // 로그인 시도한 사용자가 있을 경우 로그인 시도 이력에 사용자 ID도 저장해야함
        User user = userMapper.findUserWithCredential(loginParam.getLoginId())
                .orElse(new User()); // 없으면 비어있는 사용자 생성

        if (userLoginAttempt == null) { // 로그인 시도 이력이 없을 경우 새로 저장 (최초로그인, ID 오입력 또는 공격일 수 있음)
            userLoginAttempt = UserLoginAttempt.builder()
                    .loginId(loginParam.getLoginId()) // loginId = 사용자가 로그인을 시도한 id
                    .userId(user.getUserId()) // userId = 실제 사용자 id
                    .passwordErrorCount(0)
                    .build();
            loginAttemptTxService.saveNewLoginAttempt(userLoginAttempt); // 로그인 시도 성공/실패 여부와 상관 없는 별도 트랜잭션으로 분리
        }

        if (userLoginAttempt.getPasswordErrorCount() >= 5) {
            throw CoreException.of(ErrorType.USER_ACCOUNT_LOCKED, "비밀번호 입력 오류가 5회 누적되어 계정 이용이 제한되었습니다. '비밀번호 재설정'을 진행해 주세요.");
        }

        // 계정이 조회되지 않았거나 비밀번호가 올바르지 않다면
        if (!passwordManager.matches(loginParam.getPassword(), user.getPasswordHash())) {
            // 로그인 실패 시도횟수 + 1
            // 로그인 시도 성공/실패 여부와 상관 없는 별도 트랜잭션으로 분리
            loginAttemptTxService.updatePasswordErrorCountById(loginParam.getLoginId(), userLoginAttempt.getPasswordErrorCount() + 1);
            String unauthorizedMessage = "ID 또는 비밀번호가 올바르지 않습니다.";
            throw new CoreException(ErrorType.UNAUTHORIZED, unauthorizedMessage); // 문구 통일 "ID 또는 비밀번호가 올바르지 않습니다."
        }

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            String detail = switch (user.getAccountStatus()) {
                case PENDING -> "승인 대기 중인 계정입니다.";
                case REJECTED -> "가입 신청이 반려된 계정입니다.";
                case DISABLED -> "비활성화된 계정입니다.";
                case null, default -> "사용할 수 없는 계정입니다.";
            };
            throw CoreException.of(ErrorType.USER_ACCOUNT_LOCKED, detail);
        }

        ensureSiteEnabled(user);
        attachAccessibleSites(user);

        // 관리자 비밀번호 초기화 직후: 토큰을 발급하지 않고 비밀번호 변경을 강제한다.
        if (Boolean.TRUE.equals(user.getPasswordResetRequired())) {
            throw CoreException.of(
                    ErrorType.USER_PASSWORD_RESET_REQUIRED,
                    "비밀번호 변경 후 로그인할 수 있습니다."
            );
        }

        // 여기까지 도달했다면 정상 로그인 된 케이스
        loginAttemptTxService.updatePasswordErrorCountById(user.getUserId(), 0); // 로그인 성공 시 password 오류횟수 초기화

        user.setAutoLogin(loginParam.isAutoLogin());
        return this.generateUserToken(user);
    }

    public User createUser(User user, CurrentUser currentUser) {
        String passwordHash = passwordManager.encode(user.getPassword());
        user.setPasswordHash(passwordHash);
        user.setPassword(null);
        user.setCreatedBy(currentUser.getUserId());
        user.setUpdatedBy(currentUser.getUserId());
        applyDefaultProfileAppearance(user);
        userMapper.saveUser(user);
        persistHomeSiteAccess(user);
        return cachedUserService.getUser(user.getUserId());
    }

    // 조회 시 unique 제한 이슈로 전체 테이블 조회 필요함 (site_id 제한 금지)
    private User findUserById(String userId) {
        User user = userMapper.findUserById(userId).
                orElseThrow(() -> CoreException.of(ErrorType.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"));
        attachAccessibleSites(user);
        return user;
    }
    @Transactional(readOnly = true)
    public boolean isUserIdAvailable(String userId) {
        String normalized = userId == null ? "" : userId.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        return userMapper.findUserById(normalized).isEmpty();
    }

    // 회원가입
    @Transactional
    public User signin(User userParam) {
        ensureRegistrationSiteEnabled(userParam.getSiteId());

        // userId 중복체크
        userMapper.findUserById(userParam.getUserId())
                .ifPresent(user -> {
                    throw CoreException.of(ErrorType.USERNAME_EXISTS, "사용할 수 없는 아이디입니다.");
                });

        // email 중복체크
        // TODO email 검증 프로세스가 있으면 좋겠음
        //  나중에 비밀번호 찾기 할 때 id + email 조합으로 비밀번호 초기화 + 재설정 가능
        userMapper.findUserByEmail(userParam.getUserEmail())
                .ifPresent(user -> {
                    throw CoreException.of(ErrorType.USERNAME_EXISTS, "사용할 수 없는 이메일입니다.");
                });

        // PENDING 상태의 계정 생성
        userParam.setAccountStatus(AccountStatus.PENDING); // 사용자 생성 시 Pending 상태로 생성
        userParam.setUserRole(UserRole.USER);
        userParam.setPasswordResetRequired(false);
        this.createUser(userParam);

        // TODO 비정상 login attempt 가 있을 수 있으므로 가입한 ID 기준으로 기존 loginAttempt 삭제 or 초기화 로직 추가

        // 여기까지 왔다면 정상적으로 insert가 완료된 상태
        // findById 결과가 없다면 무언가 잘못된 상태. 전체 쿼리 트레이스를 봐야함
        return findUserById(userParam.getUserId());

    }

    public void createUser(User user) {
        var passwordHash = passwordManager.encode(user.getPassword());
        user.setPasswordHash(passwordHash);
        user.setPassword(null);
        if (user.getAccountStatus() == null) { // Status 지정이 없으면 PENDING 상태로 생성
            user.setAccountStatus(AccountStatus.PENDING);
        }
        user.setCreatedBy(ANONYMOUS_ACTOR);
        user.setUpdatedBy(ANONYMOUS_ACTOR);
        applyDefaultProfileAppearance(user);
        userMapper.saveUser(user);
        persistHomeSiteAccess(user);
    }


    @Transactional(readOnly = true)
    public List<User> getManageableUsers() {
        AuthUtil.ensureUserAdmin();
        var currentUser = AuthUtil.getCurrentUser();
        String siteId = currentUser.getUserSiteId();
        List<User> users = userMapper.findAllUsers(siteId);
        attachAccessibleSites(users);
        return users;
    }

    @Transactional(readOnly = true)
    public UserPage getManageableUsers(
            int requestedPage,
            int size,
            String query,
            AccountStatus status,
            UserRole role,
            String requestedSiteId
    ) {
        AuthUtil.ensureUserAdmin();
        var currentUser = AuthUtil.getCurrentUser();
        String siteId = currentUser.getUserSiteId();
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        long totalElements = userMapper.countUsers(siteId, normalizedQuery, status, role);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int page = totalPages == 0 ? 1 : Math.min(requestedPage, totalPages);
        long offset = (long) (page - 1) * size;
        var statusCounts = new EnumMap<AccountStatus, Long>(AccountStatus.class);
        for (UserStatusCount count : userMapper.countUsersByStatus(siteId, normalizedQuery, role)) {
            statusCounts.put(count.getAccountStatus(), count.getCount());
        }
        var content = totalElements == 0 ? List.<User>of()
                : userMapper.findUsersPage(siteId, normalizedQuery, status, role, size, offset);
        attachAccessibleSites(content);
        return new UserPage(content, page, size, totalElements, totalPages, statusCounts);
    }

    @Transactional
    public int bulkAction(List<String> userIds, UserBulkAction action, String reason) {
        AuthUtil.ensureUserAdmin();
        for (String userId : userIds) {
            var beforeUser = getManageableUser(userId);
            AccountStatus beforeStatus = beforeUser.getAccountStatus();
            User updatedUser;

            switch (action) {
                case APPROVE -> {
                    if (beforeStatus != AccountStatus.PENDING) {
                        throw CoreException.of(ErrorType.INVALID_DATA, "승인 대기 중인 계정만 승인할 수 있습니다.");
                    }
                    updatedUser = approveUser(
                            userId,
                            beforeUser.getUsername(),
                            beforeUser.getUserEmail(),
                            beforeUser.resolveSiteIds(),
                            beforeUser.getUserRole()
                    );
                }
                case REJECT -> {
                    if (beforeStatus != AccountStatus.PENDING) {
                        throw CoreException.of(ErrorType.INVALID_DATA, "승인 대기 중인 계정만 반려할 수 있습니다.");
                    }
                    updatedUser = updateUserStatus(userId, AccountStatus.REJECTED);
                }
                case DISABLE -> {
                    ensureNotSuperUserForDisable(beforeUser);
                    if (beforeStatus != AccountStatus.ACTIVE) {
                        throw CoreException.of(ErrorType.INVALID_DATA, "활성 계정만 비활성화할 수 있습니다.");
                    }
                    updatedUser = updateUserStatus(userId, AccountStatus.DISABLED);
                }
                default -> throw CoreException.of(ErrorType.INVALID_DATA, "지원하지 않는 일괄 작업입니다.");
            }

            auditService.recordChanges(
                    updatedUser.getSiteId(),
                    "USER",
                    updatedUser.getUserId(),
                    AuditAction.UPDATE,
                    reason,
                    Map.of("accountStatus", beforeStatus.name()),
                    Map.of("accountStatus", updatedUser.getAccountStatus().name()),
                    List.of("accountStatus")
            );
        }
        return userIds.size();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void applyDefaultProfileAppearance(User user) {
        if (user.getProfileColor() == null || user.getProfileColor().isBlank()) {
            user.setProfileColor(PROFILE_COLOR_PRESETS.get(
                    ThreadLocalRandom.current().nextInt(PROFILE_COLOR_PRESETS.size())
            ));
        } else {
            user.setProfileColor(normalizeProfileColor(user.getProfileColor()));
        }
        if (user.getProfileInitials() == null || user.getProfileInitials().isBlank()) {
            user.setProfileInitials(firstCharacter(
                    user.getUsername() == null || user.getUsername().isBlank()
                            ? user.getUserId()
                            : user.getUsername()
            ));
        } else {
            user.setProfileInitials(normalizeProfileInitials(user.getProfileInitials()));
        }
    }

    private String normalizeProfileColor(String profileColor) {
        String normalized = profileColor == null ? "" : profileColor.trim().toUpperCase(Locale.ROOT);
        if (!PROFILE_COLOR_PATTERN.matcher(normalized).matches()) {
            throw CoreException.of(ErrorType.INVALID_DATA, "프로필 색상 형식이 올바르지 않습니다.");
        }
        return normalized;
    }

    private String normalizeProfileInitials(String profileInitials) {
        String normalized = profileInitials == null ? "" : profileInitials.trim();
        int length = normalized.codePointCount(0, normalized.length());
        if (length < 1 || length > 2) {
            throw CoreException.of(ErrorType.INVALID_DATA, "프로필 이니셜은 1~2글자로 입력해 주세요.");
        }
        return normalized;
    }

    private String firstCharacter(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            return "?";
        }
        return normalized.substring(0, normalized.offsetByCodePoints(0, 1));
    }

    @Transactional(readOnly = true)
    public User getManageableUser(String userId) {
        AuthUtil.ensureUserAdmin();
        var user = this.findUserById(userId);
        AuthUtil.ensureCanManageUser(user);
        // PENDING은 승인 시 사이트를 새로 부여하므로, 홈 site_id가 비어 있어도 막지 않는다.
        if (user.getUserRole() != UserRole.SUPER
                && user.getAccountStatus() != AccountStatus.PENDING
                && !hasAnyExistingSite(user)) {
            throw CoreException.of(
                    ErrorType.SITE_NOT_FOUND,
                    "폐기된 사이트의 사용자는 변경할 수 없습니다."
            );
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User getViewableUser(String userId) {
        AuthUtil.ensureUserAdmin();
        var user = this.findUserById(userId);
        AuthUtil.ensureCanViewUser(user);
        return user;
    }

    @Transactional
    public User updateUserStatus(String userId, AccountStatus accountStatus) {
        var currentUser = AuthUtil.getCurrentUser();
        var user = getManageableUser(userId);
        return updateUserStatus(user, currentUser, accountStatus);
    }

    @Transactional
    public User updateUserStatus(String userId, AccountStatus accountStatus, String reason) {
        var currentUser = AuthUtil.getCurrentUser();
        var user = getManageableUser(userId);
        AccountStatus beforeStatus = user.getAccountStatus();
        var updatedUser = updateUserStatus(user, currentUser, accountStatus);
        auditService.recordChanges(
                updatedUser.getSiteId(),
                "USER",
                updatedUser.getUserId(),
                AuditAction.UPDATE,
                reason,
                Map.of("accountStatus", beforeStatus.name()),
                Map.of("accountStatus", updatedUser.getAccountStatus().name()),
                List.of("accountStatus")
        );
        return updatedUser;
    }

    private User updateUserStatus(User user, CurrentUser currentUser, AccountStatus accountStatus) {
        String userId = user.getUserId();

        if ((user.getAccountStatus() == AccountStatus.PENDING || user.getAccountStatus() == AccountStatus.REJECTED)
                && accountStatus == AccountStatus.ACTIVE) {
            throw CoreException.of(ErrorType.INVALID_DATA, "가입 승인 정보 확인 후 승인해 주세요.");
        }

        if (currentUser.getUserId().equals(userId)
                && accountStatus != AccountStatus.ACTIVE) {
            throw CoreException.of(ErrorType.INVALID_DATA, "본인 계정은 비활성화하거나 반려할 수 없습니다.");
        }
        if (accountStatus == AccountStatus.DISABLED) {
            ensureNotSuperUserForDisable(user);
        }

        user.setAccountStatus(accountStatus);
        user.setUpdatedBy(currentUser.getUserId());
        userMapper.updateUserStatus(user);
        return findUserById(userId);
    }

    @Transactional
    public User approveUser(
            String userId,
            String username,
            String userEmail,
            List<String> siteIds,
            UserRole userRole
    ) {
        AuthUtil.ensureUserAdmin();
        var currentUser = AuthUtil.getCurrentUser();
        var user = getManageableUser(userId);
        return approveUser(user, currentUser, username, userEmail, siteIds, userRole);
    }

    @Transactional
    public User approveUser(
            String userId,
            String username,
            String userEmail,
            List<String> siteIds,
            UserRole userRole,
            String reason
    ) {
        AuthUtil.ensureUserAdmin();
        var currentUser = AuthUtil.getCurrentUser();
        var user = getManageableUser(userId);
        Map<String, Object> before = approvalAuditedValues(user);
        var approvedUser = approveUser(user, currentUser, username, userEmail, siteIds, userRole);
        auditService.recordChanges(
                approvedUser.getSiteId(),
                "USER",
                approvedUser.getUserId(),
                AuditAction.UPDATE,
                reason,
                before,
                approvalAuditedValues(approvedUser),
                List.of("accountStatus", "username", "userEmail", "siteIds", "userRole")
        );
        return approvedUser;
    }

    private User approveUser(
            User user,
            CurrentUser currentUser,
            String username,
            String userEmail,
            List<String> siteIds,
            UserRole userRole
    ) {
        String userId = user.getUserId();

        if (user.getAccountStatus() != AccountStatus.PENDING) {
            throw CoreException.of(ErrorType.INVALID_DATA, "승인 대기 중인 계정만 승인할 수 있습니다.");
        }
        if (userRole == UserRole.GUEST) {
            throw CoreException.of(ErrorType.INVALID_DATA, "GUEST 권한은 승인할 수 없습니다.");
        }
        if (currentUser.getUserRole() == UserRole.SITE_ADMIN
                && userRole != UserRole.USER && userRole != UserRole.SITE_ADMIN) {
            throw CoreException.of(ErrorType.FORBIDDEN, "슈퍼유저 권한은 부여할 수 없습니다.");
        }

        List<String> mergedSiteIds = mergeGrantedSiteIds(currentUser, user, siteIds);
        String homeSiteId = resolveHomeSiteId(user, mergedSiteIds);
        if (homeSiteId == null || homeSiteId.isBlank()) {
            throw CoreException.of(ErrorType.INVALID_DATA, "사이트를 하나 이상 부여해야 합니다.");
        }
        userMapper.findUserByEmail(userEmail)
                .filter(existing -> !existing.getUserId().equals(userId))
                .ifPresent(existing -> {
                    throw CoreException.of(ErrorType.USERNAME_EXISTS, "이미 사용 중인 이메일입니다.");
                });

        user.setUsername(username);
        user.setUserEmail(userEmail);
        user.setSiteId(homeSiteId);
        user.setUserRole(userRole);
        user.setUpdatedBy(currentUser.getUserId());
        if (userMapper.approveUser(user) != 1) {
            throw CoreException.of(ErrorType.INVALID_DATA, "승인 대기 중인 계정만 승인할 수 있습니다.");
        }
        replaceSiteAccess(user.getAccountId(), mergedSiteIds);
        user.setAccountStatus(AccountStatus.ACTIVE);
        return findUserById(userId);
    }

    private void ensureNotSuperUserForDisable(User user) {
        if (user.getUserRole() == UserRole.SUPER) {
            throw CoreException.of(ErrorType.INVALID_DATA, "SUPER 계정은 비활성화할 수 없습니다.");
        }
    }

    private Map<String, Object> approvalAuditedValues(User user) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("accountStatus", user.getAccountStatus() == null ? null : user.getAccountStatus().name());
        values.put("username", user.getUsername());
        values.put("userEmail", user.getUserEmail());
        values.put("siteIds", user.resolveSiteIds());
        values.put("userRole", user.getUserRole() == null ? null : user.getUserRole().name());
        return values;
    }

    @Transactional
    public User updateManagedUser(
            String userId,
            String username,
            String userEmail,
            List<String> siteIds,
            UserRole userRole
    ) {
        AuthUtil.ensureUserAdmin();
        var currentUser = AuthUtil.getCurrentUser();
        var user = getManageableUser(userId);
        Map<String, Object> before = approvalAuditedValues(user);

        if (user.getAccountStatus() != AccountStatus.ACTIVE && user.getAccountStatus() != AccountStatus.DISABLED) {
            throw CoreException.of(ErrorType.INVALID_DATA, "승인 완료 또는 비활성화된 계정만 수정할 수 있습니다.");
        }
        if (userRole == UserRole.GUEST) {
            throw CoreException.of(ErrorType.INVALID_DATA, "GUEST 권한은 부여할 수 없습니다.");
        }

        boolean isSelf = currentUser.getUserId().equals(userId);
        List<String> requestedSiteIds = siteIds;
        if (isSelf) {
            requestedSiteIds = user.resolveSiteIds();
            userRole = user.getUserRole();
        }
        UserRole targetUserRole = userRole;

        if (currentUser.getUserRole() == UserRole.SITE_ADMIN
                && targetUserRole != UserRole.USER && targetUserRole != UserRole.SITE_ADMIN) {
            throw CoreException.of(ErrorType.FORBIDDEN, "슈퍼유저 권한은 부여할 수 없습니다.");
        }

        List<String> mergedSiteIds = mergeGrantedSiteIds(currentUser, user, requestedSiteIds);
        String homeSiteId = resolveHomeSiteId(user, mergedSiteIds);
        if (homeSiteId == null || homeSiteId.isBlank()) {
            throw CoreException.of(ErrorType.INVALID_DATA, "사이트를 하나 이상 부여해야 합니다.");
        }
        userMapper.findUserByEmail(userEmail)
                .filter(existing -> !existing.getUserId().equals(userId))
                .ifPresent(existing -> {
                    throw CoreException.of(ErrorType.USERNAME_EXISTS, "이미 사용 중인 이메일입니다.");
                });

        user.setUsername(username);
        user.setUserEmail(userEmail);
        user.setSiteId(homeSiteId);
        user.setUserRole(targetUserRole);
        user.setUpdatedBy(currentUser.getUserId());
        if (userMapper.updateManagedUser(user) != 1) {
            throw CoreException.of(ErrorType.INVALID_DATA, "승인 완료 또는 비활성화된 계정만 수정할 수 있습니다.");
        }
        replaceSiteAccess(user.getAccountId(), mergedSiteIds);
        User updatedUser = findUserById(userId);
        auditService.recordChanges(
                updatedUser.getSiteId(),
                "USER",
                updatedUser.getUserId(),
                AuditAction.UPDATE,
                null,
                before,
                approvalAuditedValues(updatedUser),
                MANAGED_USER_AUDIT_FIELDS
        );
        return updatedUser;
    }

    @Transactional
    public User resetUserPassword(String userId, String newPassword) {
        var currentUser = AuthUtil.getCurrentUser();
        if (currentUser.getUserId().equals(userId)) {
            throw CoreException.of(ErrorType.INVALID_DATA, "본인 비밀번호는 내 계정에서 변경해 주세요.");
        }
        var user = getManageableUser(userId);

        user.setPasswordHash(passwordManager.encode(newPassword));
        user.setUpdatedBy(currentUser.getUserId());
        userMapper.resetUserPassword(user);
        loginAttemptTxService.updatePasswordErrorCountById(userId, 0);

        User updatedUser = findUserById(userId);
        auditService.recordChanges(
                updatedUser.getSiteId(),
                "USER",
                updatedUser.getUserId(),
                AuditAction.UPDATE,
                null,
                Map.of("passwordReset", false),
                Map.of("passwordReset", true),
                List.of("passwordReset")
        );
        return updatedUser;
    }

    @Transactional
    public User updateMyProfile(
            CurrentUser currentUser,
            String username,
            String userEmail,
            String phoneNumber,
            String profileColor,
            String profileInitials
    ) {
        var user = findUserById(currentUser.getUserId());
        userMapper.findUserByEmail(userEmail)
                .filter(existing -> !existing.getUserId().equals(currentUser.getUserId()))
                .ifPresent(existing -> {
                    throw CoreException.of(ErrorType.USERNAME_EXISTS, "이미 사용 중인 이메일입니다.");
                });

        user.setUsername(username);
        user.setUserEmail(userEmail);
        user.setPhoneNumber(phoneNumber == null || phoneNumber.isBlank() ? null : phoneNumber);
        user.setProfileColor(normalizeProfileColor(profileColor));
        user.setProfileInitials(normalizeProfileInitials(profileInitials));
        user.setUpdatedBy(currentUser.getUserId());
        userMapper.updateUserProfile(user);
        return findUserById(currentUser.getUserId());
    }

    @Transactional
    public User changeMyPassword(
            CurrentUser currentUser,
            String currentPassword,
            String newPassword
    ) {
        var userWithCredential = userMapper.findUserWithCredential(currentUser.getUserId())
                .orElseThrow(() -> CoreException.of(ErrorType.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"));

        if (!passwordManager.matches(currentPassword, userWithCredential.getPasswordHash())) {
            throw CoreException.of(ErrorType.UNAUTHORIZED, "현재 비밀번호가 올바르지 않습니다.");
        }
        if (passwordManager.matches(newPassword, userWithCredential.getPasswordHash())) {
            throw CoreException.of(ErrorType.INVALID_DATA, "현재 비밀번호와 다른 비밀번호를 입력해 주세요.");
        }

        userWithCredential.setPasswordHash(passwordManager.encode(newPassword));
        userWithCredential.setUpdatedBy(currentUser.getUserId());
        userMapper.updateUserPassword(userWithCredential);
        loginAttemptTxService.updatePasswordErrorCountById(currentUser.getUserId(), 0);
        return findUserById(currentUser.getUserId());
    }

    /**
     * 로그인 전(비인증) 강제 비밀번호 변경.
     * password_reset_required 계정만 허용하며, 토큰은 발급하지 않는다.
     */
    @Transactional
    public void changePasswordWhenResetRequired(String loginId, String currentPassword, String newPassword) {
        var userLoginAttempt = userMapper.findUserLoginAttempt(
                UserLoginAttempt.builder().loginId(loginId).build()
        );
        User user = userMapper.findUserWithCredential(loginId).orElse(new User());

        if (userLoginAttempt == null) {
            userLoginAttempt = UserLoginAttempt.builder()
                    .loginId(loginId)
                    .userId(user.getUserId())
                    .passwordErrorCount(0)
                    .build();
            loginAttemptTxService.saveNewLoginAttempt(userLoginAttempt);
        }

        if (userLoginAttempt.getPasswordErrorCount() >= 5) {
            throw CoreException.of(
                    ErrorType.USER_ACCOUNT_LOCKED,
                    "비밀번호 입력 오류가 5회 누적되어 계정 이용이 제한되었습니다. '비밀번호 재설정'을 진행해 주세요."
            );
        }

        if (!passwordManager.matches(currentPassword, user.getPasswordHash())) {
            loginAttemptTxService.updatePasswordErrorCountById(loginId, userLoginAttempt.getPasswordErrorCount() + 1);
            throw CoreException.of(ErrorType.UNAUTHORIZED, "ID 또는 비밀번호가 올바르지 않습니다.");
        }

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw CoreException.of(ErrorType.USER_ACCOUNT_LOCKED, "사용할 수 없는 계정입니다.");
        }

        ensureSiteEnabled(user);

        if (!Boolean.TRUE.equals(user.getPasswordResetRequired())) {
            throw CoreException.of(ErrorType.INVALID_DATA, "비밀번호 초기화가 필요하지 않은 계정입니다.");
        }

        if (passwordManager.matches(newPassword, user.getPasswordHash())) {
            throw CoreException.of(ErrorType.INVALID_DATA, "현재 비밀번호와 다른 비밀번호를 입력해 주세요.");
        }

        user.setPasswordHash(passwordManager.encode(newPassword));
        user.setUpdatedBy(user.getUserId());
        userMapper.updateUserPassword(user);
        loginAttemptTxService.updatePasswordErrorCountById(loginId, 0);
    }

    public UserToken refresh(String refreshToken, Boolean autoLogin) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "Refresh Token is Invalid or expired");
        }
        var currentUser = jwtUtil.getCurrentUserFromToken(refreshToken);
        User user = userMapper.findUserById(currentUser.getUserId())
                .orElseThrow(() -> CoreException.of(ErrorType.UNAUTHORIZED, "Invalid Token ID"));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "사용할 수 없는 계정입니다.");
        }

        // 초기화 필요 계정은 세션 복원을 허용하지 않는다.
        if (Boolean.TRUE.equals(user.getPasswordResetRequired())) {
            throw CoreException.of(
                    ErrorType.USER_PASSWORD_RESET_REQUIRED,
                    "비밀번호 변경 후 로그인할 수 있습니다."
            );
        }

        ensureSiteEnabled(user);

        user.setAutoLogin(autoLogin);
        return this.generateUserToken(user);
    }

    private void ensureSiteEnabled(User user) {
        if (user.getUserRole() == UserRole.SUPER) {
            return;
        }

        boolean anyEnabled = loadAccessibleSiteIds(user).stream()
                .map(siteId -> siteMapper.findSiteById(SiteInfo.builder().siteId(siteId).build()))
                .flatMap(java.util.Optional::stream)
                .anyMatch(site -> Boolean.TRUE.equals(site.getSiteEnabled()));
        if (!anyEnabled) {
            throw CoreException.of(
                    ErrorType.FORBIDDEN,
                    "비활성화된 사이트의 계정은 로그인할 수 없습니다."
            );
        }
    }

    private List<String> loadAccessibleSiteIds(User user) {
        if (user.getSiteIds() != null && !user.getSiteIds().isEmpty()) {
            return user.getSiteIds();
        }
        if (user.getAccountId() != null) {
            List<String> stored = userMapper.findSiteIdsByAccountId(user.getAccountId());
            if (stored != null && !stored.isEmpty()) {
                return stored;
            }
        }
        return user.resolveSiteIds();
    }

    private void attachAccessibleSites(User user) {
        if (user == null) {
            return;
        }
        attachAccessibleSites(List.of(user));
    }

    private void attachAccessibleSites(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> accountIds = users.stream()
                .map(User::getAccountId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, List<UserSite>> sitesByAccount = new LinkedHashMap<>();
        if (!accountIds.isEmpty()) {
            List<UserSite> loaded = userMapper.findAccessibleSitesByAccountIds(accountIds);
            if (loaded != null) {
                for (UserSite site : loaded) {
                    if (site.getAccountId() == null) {
                        continue;
                    }
                    sitesByAccount.computeIfAbsent(site.getAccountId(), ignored -> new ArrayList<>()).add(site);
                }
            }
        }
        for (User user : users) {
            List<UserSite> sites = user.getAccountId() == null
                    ? List.of()
                    : sitesByAccount.getOrDefault(user.getAccountId(), List.of());
            user.setSites(sites);
            user.setSiteIds(sites.stream().map(UserSite::getSiteId).filter(Objects::nonNull).toList());
        }
    }

    private boolean hasAnyExistingSite(User user) {
        return loadAccessibleSiteIds(user).stream()
                .map(siteId -> siteMapper.findSiteById(SiteInfo.builder().siteId(siteId).build()))
                .anyMatch(java.util.Optional::isPresent);
    }

    private List<String> mergeGrantedSiteIds(CurrentUser actor, User target, List<String> requestedSiteIds) {
        List<String> requested = normalizeSiteIds(requestedSiteIds);
        if (requested.isEmpty()) {
            throw CoreException.of(ErrorType.INVALID_DATA, "사이트를 하나 이상 부여해야 합니다.");
        }
        validateSitesExist(requested);

        if (actor.getUserRole() == UserRole.SUPER) {
            return requested;
        }

        Set<String> adminSites = new LinkedHashSet<>(actor.resolveAccessibleSiteIds());
        for (String siteId : requested) {
            if (!adminSites.contains(siteId)) {
                throw CoreException.of(ErrorType.FORBIDDEN, "자신의 사이트만 부여할 수 있습니다.");
            }
        }

        Set<String> merged = new LinkedHashSet<>(requested);
        for (String existing : target.resolveSiteIds()) {
            if (!adminSites.contains(existing)) {
                merged.add(existing);
            }
        }
        if (merged.isEmpty()) {
            throw CoreException.of(ErrorType.INVALID_DATA, "사이트를 하나 이상 부여해야 합니다.");
        }
        return List.copyOf(merged);
    }

    private String resolveHomeSiteId(User target, List<String> mergedSiteIds) {
        String currentHome = target.getSiteId();
        if (currentHome != null && !currentHome.isBlank() && mergedSiteIds.contains(currentHome)) {
            return currentHome;
        }
        return mergedSiteIds.isEmpty() ? null : mergedSiteIds.get(0);
    }

    private List<String> normalizeSiteIds(List<String> siteIds) {
        if (siteIds == null) {
            return List.of();
        }
        return siteIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(siteId -> !siteId.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private void validateSitesExist(List<String> siteIds) {
        for (String siteId : siteIds) {
            siteMapper.findSiteById(SiteInfo.builder().siteId(siteId).build())
                    .orElseThrow(() -> CoreException.of(ErrorType.SITE_NOT_FOUND, "잘못된 사이트 ID 입니다. " + siteId));
        }
    }

    private void persistHomeSiteAccess(User user) {
        if (user.getAccountId() == null || user.getSiteId() == null || user.getSiteId().isBlank()) {
            return;
        }
        userMapper.insertSiteAccess(
                user.getAccountId(),
                user.getSiteId(),
                user.getCreatedBy(),
                user.getCreatedIp()
        );
    }

    private void replaceSiteAccess(Long accountId, List<String> siteIds) {
        if (accountId == null) {
            return;
        }
        userMapper.deleteSiteAccessByAccountId(accountId);
        if (siteIds != null && !siteIds.isEmpty()) {
            userMapper.insertSiteAccessBatch(accountId, siteIds);
        }
    }

    private void ensureRegistrationSiteEnabled(String siteId) {
        siteMapper.findSiteById(SiteInfo.builder().siteId(siteId).build())
                .filter(site -> Boolean.TRUE.equals(site.getSiteEnabled()))
                .orElseThrow(() -> CoreException.of(ErrorType.SITE_NOT_FOUND, "잘못된 사이트 ID 입니다. " + siteId));
    }

    private UserToken generateUserToken(User user) {
        // 토큰 생성 전 명시적으로 민감정보는 삭제
        user.setPassword(null);
        user.setPasswordHash(null);
        user.setPhoneNumber(null);

        Map<String, Object> claims = new HashMap<>();
        claims.put("accountId", user.getAccountId());
        claims.put("userSiteId", user.resolveHomeSiteId());
        claims.put("userRole", user.getUserRole());

        String accessToken =  jwtUtil.generateToken(user.getUserId(), claims, accessTokenExpiration);
        String refreshToken = jwtUtil.generateToken(user.getUserId(), claims, refreshTokenExpiration);

        var refreshCookieBuilder = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .path("/")
                .sameSite("lax");
        if (user.getAutoLogin()) {
            refreshCookieBuilder.maxAge(Duration.ofSeconds(refreshTokenExpiration));
        }
        // autoLogin false -> 세션 쿠키 (브라우저 종료 시 삭제)

        return UserToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .refreshCookie(refreshCookieBuilder.build())
                .tokenType("Bearer")
                .build();
    }
}
