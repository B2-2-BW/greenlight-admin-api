package com.winten.greenlight.admin.domain.user;

import com.winten.greenlight.admin.db.repository.mapper.site.SiteMapper;
import com.winten.greenlight.admin.db.repository.mapper.user.UserMapper;
import com.winten.greenlight.admin.db.repository.mapper.user.UserStatusCount;
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
import java.util.HashMap;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final LoginAttemptTxService loginAttemptTxService;
    private final SiteMapper siteMapper;
    private final PasswordManager passwordManager;
    private final CachedUserService cachedUserService;
    private final JwtUtil jwtUtil;

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
        userMapper.saveUser(user);
        return cachedUserService.getUser(user.getUserId());
    }

    // 조회 시 unique 제한 이슈로 전체 테이블 조회 필요함 (site_id 제한 금지)
    private User findUserById(String userId) {
        return userMapper.findUserById(userId).
                orElseThrow(() -> CoreException.of(ErrorType.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"));
    }
    // 회원가입
    @Transactional
    public User signin(User userParam) {
        // 유효한 Site ID인지 검증
        siteMapper.findSiteById(SiteInfo.builder().siteId(userParam.getSiteId()).build())
                .orElseThrow(() -> CoreException.of(ErrorType.SITE_NOT_FOUND, "잘못된 사이트 ID 입니다. " + userParam.getSiteId()));

        // userId 중복체크
        userMapper.findUserById(userParam.getUserId())
                .ifPresent(user -> {
                    throw CoreException.of(ErrorType.USERNAME_EXISTS, "사용할 수 없는 계정입니다.");
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
        userParam.setCreatedBy(userParam.getUserId());
        userParam.setUpdatedBy(userParam.getUserId());
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
        userMapper.saveUser(user);
    }


    @Transactional(readOnly = true)
    public List<User> getManageableUsers() {
        AuthUtil.ensureUserAdmin();
        var currentUser = AuthUtil.getCurrentUser();
        String siteId = currentUser.getUserRole() == UserRole.SUPER
                ? null
                : currentUser.getUserSiteId();
        return userMapper.findAllUsers(siteId);
    }

    @Transactional(readOnly = true)
    public UserPage getManageableUsers(int requestedPage, int size, String query) {
        AuthUtil.ensureUserAdmin();
        var currentUser = AuthUtil.getCurrentUser();
        String siteId = currentUser.getUserRole() == UserRole.SUPER ? null : currentUser.getUserSiteId();
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        long totalElements = userMapper.countUsers(siteId, normalizedQuery);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int page = totalPages == 0 ? 1 : Math.min(requestedPage, totalPages);
        long offset = (long) (page - 1) * size;
        var statusCounts = new EnumMap<AccountStatus, Long>(AccountStatus.class);
        for (UserStatusCount count : userMapper.countUsersByStatus(siteId, normalizedQuery)) {
            statusCounts.put(count.getAccountStatus(), count.getCount());
        }
        var content = totalElements == 0 ? List.<User>of()
                : userMapper.findUsersPage(siteId, normalizedQuery, size, offset);
        return new UserPage(content, page, size, totalElements, totalPages, statusCounts);
    }

    @Transactional(readOnly = true)
    public User getManageableUser(String userId) {
        AuthUtil.ensureUserAdmin();
        var user = this.findUserById(userId);
        AuthUtil.ensureCanManageUser(user.getSiteId(), user.getUserRole());
        return user;
    }

    @Transactional(readOnly = true)
    public User getViewableUser(String userId) {
        AuthUtil.ensureUserAdmin();
        var user = this.findUserById(userId);
        AuthUtil.ensureCanViewUser(user.getSiteId());
        return user;
    }

    @Transactional
    public User updateUserStatus(String userId, AccountStatus accountStatus) {
        var currentUser = AuthUtil.getCurrentUser();
        var user = getManageableUser(userId);

        if ((user.getAccountStatus() == AccountStatus.PENDING || user.getAccountStatus() == AccountStatus.REJECTED)
                && accountStatus == AccountStatus.ACTIVE) {
            throw CoreException.of(ErrorType.INVALID_DATA, "가입 승인 정보 확인 후 승인해 주세요.");
        }

        if (currentUser.getUserId().equals(userId)
                && accountStatus != AccountStatus.ACTIVE) {
            throw CoreException.of(ErrorType.INVALID_DATA, "본인 계정은 비활성화하거나 반려할 수 없습니다.");
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
            String siteId,
            UserRole userRole
    ) {
        AuthUtil.ensureUserAdmin();
        var currentUser = AuthUtil.getCurrentUser();
        var user = getManageableUser(userId);

        if (user.getAccountStatus() != AccountStatus.PENDING) {
            throw CoreException.of(ErrorType.INVALID_DATA, "승인 대기 중인 계정만 승인할 수 있습니다.");
        }
        if (userRole == UserRole.GUEST) {
            throw CoreException.of(ErrorType.INVALID_DATA, "GUEST 권한은 승인할 수 없습니다.");
        }
        if (currentUser.getUserRole() == UserRole.SITE_ADMIN) {
            if (!currentUser.getUserSiteId().equals(siteId)) {
                throw CoreException.of(ErrorType.FORBIDDEN, "자신의 사이트 계정만 승인할 수 있습니다.");
            }
            if (userRole != UserRole.USER && userRole != UserRole.SITE_ADMIN) {
                throw CoreException.of(ErrorType.FORBIDDEN, "슈퍼유저 권한은 부여할 수 없습니다.");
            }
        }

        siteMapper.findSiteById(SiteInfo.builder().siteId(siteId).build())
                .orElseThrow(() -> CoreException.of(ErrorType.SITE_NOT_FOUND, "잘못된 사이트 ID 입니다. " + siteId));
        userMapper.findUserByEmail(userEmail)
                .filter(existing -> !existing.getUserId().equals(userId))
                .ifPresent(existing -> {
                    throw CoreException.of(ErrorType.USERNAME_EXISTS, "이미 사용 중인 이메일입니다.");
                });

        user.setUsername(username);
        user.setUserEmail(userEmail);
        user.setSiteId(siteId);
        user.setUserRole(userRole);
        user.setUpdatedBy(currentUser.getUserId());
        if (userMapper.approveUser(user) != 1) {
            throw CoreException.of(ErrorType.INVALID_DATA, "승인 대기 중인 계정만 승인할 수 있습니다.");
        }
        user.setAccountStatus(AccountStatus.ACTIVE);
        return findUserById(userId);
    }

    @Transactional
    public User updateManagedUser(
            String userId,
            String username,
            String userEmail,
            String siteId,
            UserRole userRole
    ) {
        AuthUtil.ensureUserAdmin();
        var currentUser = AuthUtil.getCurrentUser();
        var user = getManageableUser(userId);

        if (user.getAccountStatus() != AccountStatus.ACTIVE && user.getAccountStatus() != AccountStatus.DISABLED) {
            throw CoreException.of(ErrorType.INVALID_DATA, "승인 완료 또는 비활성화된 계정만 수정할 수 있습니다.");
        }
        if (userRole == UserRole.GUEST) {
            throw CoreException.of(ErrorType.INVALID_DATA, "GUEST 권한은 부여할 수 없습니다.");
        }

        boolean isSelf = currentUser.getUserId().equals(userId);
        if (isSelf) {
            siteId = user.getSiteId();
            userRole = user.getUserRole();
        }
        String targetSiteId = siteId;
        UserRole targetUserRole = userRole;

        if (currentUser.getUserRole() == UserRole.SITE_ADMIN) {
            if (!currentUser.getUserSiteId().equals(targetSiteId)) {
                throw CoreException.of(ErrorType.FORBIDDEN, "자신의 사이트 계정만 수정할 수 있습니다.");
            }
            if (targetUserRole != UserRole.USER && targetUserRole != UserRole.SITE_ADMIN) {
                throw CoreException.of(ErrorType.FORBIDDEN, "슈퍼유저 권한은 부여할 수 없습니다.");
            }
        }

        siteMapper.findSiteById(SiteInfo.builder().siteId(targetSiteId).build())
                .orElseThrow(() -> CoreException.of(ErrorType.SITE_NOT_FOUND, "잘못된 사이트 ID 입니다. " + targetSiteId));
        userMapper.findUserByEmail(userEmail)
                .filter(existing -> !existing.getUserId().equals(userId))
                .ifPresent(existing -> {
                    throw CoreException.of(ErrorType.USERNAME_EXISTS, "이미 사용 중인 이메일입니다.");
                });

        user.setUsername(username);
        user.setUserEmail(userEmail);
        user.setSiteId(targetSiteId);
        user.setUserRole(targetUserRole);
        user.setUpdatedBy(currentUser.getUserId());
        if (userMapper.updateManagedUser(user) != 1) {
            throw CoreException.of(ErrorType.INVALID_DATA, "승인 완료 또는 비활성화된 계정만 수정할 수 있습니다.");
        }
        return findUserById(userId);
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

        return findUserById(userId);
    }

    @Transactional
    public User updateMyProfile(
            CurrentUser currentUser,
            String username,
            String userEmail,
            String phoneNumber
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

        user.setAutoLogin(autoLogin);
        return this.generateUserToken(user);
    }

    private UserToken generateUserToken(User user) {
        // 토큰 생성 전 명시적으로 민감정보는 삭제
        user.setPassword(null);
        user.setPasswordHash(null);
        user.setPhoneNumber(null);

        Map<String, Object> claims = new HashMap<>();
        claims.put("accountId", user.getAccountId());
        claims.put("userSiteId", user.getSiteId());
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
