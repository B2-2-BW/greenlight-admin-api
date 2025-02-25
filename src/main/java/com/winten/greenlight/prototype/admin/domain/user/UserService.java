package com.winten.greenlight.prototype.admin.domain.user;

import com.winten.greenlight.prototype.admin.db.repository.mapper.UserMapper;
import com.winten.greenlight.prototype.admin.support.error.CoreException;
import com.winten.greenlight.prototype.admin.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final AuthManager authManager;

    public User getUser(User user) {
        return userMapper.findOne(user)
                .orElseThrow(() -> CoreException.of(ErrorType.USER_NOT_FOUND, "username이 없습니다."));
    }

    public User createUser(User user) {
        String passwordHash = authManager.encode(user.getPassword());
        user.setPasswordHash(passwordHash);
        user.setCreatedBy("SYSTEM");
        user.setUpdatedBy("SYSTEM");
        userMapper.save(user);
        return user;
    }
}