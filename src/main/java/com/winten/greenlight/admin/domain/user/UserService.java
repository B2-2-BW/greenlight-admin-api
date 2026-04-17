package com.winten.greenlight.admin.domain.user;

import com.winten.greenlight.admin.db.repository.mapper.site.SiteMapper;
import com.winten.greenlight.admin.db.repository.mapper.user.UserMapper;
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

        // 계정은 조회됐는데 password_reset_required 가 null이거나 true인 경우 비밀번호 세팅 필수
        // false인 경우에만 초기화 불필요
        if (user.getUserId() != null && (user.getPasswordResetRequired() == null || user.getPasswordResetRequired())) {
            throw CoreException.of(ErrorType.USER_ACCOUNT_LOCKED, "비밀번호 재설정이 완료되지 않아 로그인할 수 없습니다. 비밀번호를 재설정한 후 다시 시도해 주세요.");
        }
        
        // 계정이 조회되지 않았거나 비밀번호가 올바르지 않다면
        if (!passwordManager.matches(loginParam.getPassword(), user.getPasswordHash())) {
            // 로그인 실패 시도횟수 + 1
            // 로그인 시도 성공/실패 여부와 상관 없는 별도 트랜잭션으로 분리
            loginAttemptTxService.updatePasswordErrorCountById(loginParam.getLoginId(), userLoginAttempt.getPasswordErrorCount() + 1);
            String unauthorizedMessage = "ID 또는 비밀번호가 올바르지 않습니다.";
            throw new CoreException(ErrorType.UNAUTHORIZED, unauthorizedMessage); // 문구 통일 "ID 또는 비밀번호가 올바르지 않습니다."
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
        this.createUser(userParam);

        // TODO 비정상 login attempt 가 있을 수 있으므로 가입한 ID 기준으로 기존 loginAttempt 삭제 or 초기화 로직 추가

        // 여기까지 왔다면 정상적으로 insert가 완료된 상태
        // findById 결과가 없다면 무언가 잘못된 상태. 전체 쿼리 트레이스를 봐야함
        return findUserById(userParam.getUserId());

    }

    public void createUser(User user) {
        var passwordHash = passwordManager.encode(user.getPassword());
        user.setPasswordHash(passwordHash);
        if (user.getAccountStatus() == null) { // Status 지정이 없으면 PENDING 상태로 생성
            user.setAccountStatus(AccountStatus.PENDING);
        }
        userMapper.saveUser(user);
    }


    // TODO 사용자 가입 승인/반려 처리
    //  관리자 이상 사용가능 
    public User updateUserStatus(User userParam) {
        // TODO user 존재여부 확인

        // TODO user 상태 확인 PENDING인지 REJECTED 인지 등

        // TODO user 상태 업데이트 후 반환

        return null;
    }
    

    // TODO 사용자 비밀번호 null 처리 및 오류횟수 초기화
    //  본인이거나, 관리자 이상 사용가능
    public User forcePasswordReset(User userParam, CurrentUser currentUser) {
        var user = this.findUserById(userParam.getUserId());

        // 본인 Site가 아닐 경우 수정하면 안되므로 검증로직 추가 (SUPER 권한 제외)
        // 본인이 아닌 경우 해당 사이트 관리자 이상이어야 한다.
        if (!currentUser.getUserId().equals(user.getUserId())) {
            AuthUtil.ensureCanUpdate(user.getSiteId());
        }
        userMapper.resetUserPassword(user);

        // TODO 비즈니스 로직 어떻게하지?
        //  관리자는 null 처리니까 force 시키고,
        //  일반사용자는 재설정 처리 하도록 만들어도 괜찮을듯
        return null;
    }

    public UserToken refresh(String refreshToken, Boolean autoLogin) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "Refresh Token is Invalid or expired");
        }
        var currentUser = jwtUtil.getCurrentUserFromToken(refreshToken);
        User user = userMapper.findUserById(currentUser.getUserId())
                .orElseThrow(() -> CoreException.of(ErrorType.UNAUTHORIZED, "Invalid Token ID"));

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