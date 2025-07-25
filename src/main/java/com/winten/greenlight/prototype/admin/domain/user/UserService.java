package com.winten.greenlight.prototype.admin.domain.user;

import com.winten.greenlight.prototype.admin.db.repository.mapper.user.UserMapper;
import com.winten.greenlight.prototype.admin.support.error.CoreException;
import com.winten.greenlight.prototype.admin.support.error.ErrorType;
import com.winten.greenlight.prototype.admin.support.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final PasswordManager passwordManager;
    private final CachedUserService cachedUserService;
    private final JwtUtil jwtUtil;

    public User getUserWithCredential(String userId) {
        return userMapper.findUserWithCredential(userId)
                .orElseThrow(() -> CoreException.of(ErrorType.USER_NOT_FOUND, "존재하지 않는 사용자입니다."));
    }

    public UserToken login(User paramUser) {
        User user = getUserWithCredential(paramUser.getUserId());
        if (!passwordManager.matches(paramUser.getPassword(), user.getPasswordHash())) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "비밀번호가 올바르지 않습니다.");
        }
        return jwtUtil.generateToken(user);
    }

    public User createUser(User user, CurrentUser currentUser) {
        String passwordHash = passwordManager.encode(user.getPassword());
        user.setPasswordHash(passwordHash);
        user.setPassword(null);
        user.setCreatedBy(currentUser.getUserId());
        user.setUpdatedBy(currentUser.getUserId());
        userMapper.save(user);
        return cachedUserService.getUser(user.getUserId());
    }

    public User getUserAccountIdByKey(String apiKey) {
        return userMapper.findUserAccountIdByApiKey(apiKey)
                .orElseThrow(() -> CoreException.of(ErrorType.UNAUTHORIZED, "유효하지 않은 API Key 입니다."));
    }
}