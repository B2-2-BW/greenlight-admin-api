package com.winten.greenlight.admin.domain.user;

import com.winten.greenlight.admin.db.repository.mapper.user.UserMapper;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CachedUserService {
    private final UserMapper userMapper;

    public User getUser(String userId) {
        return userMapper.findOne(userId)
                .orElseThrow(() -> CoreException.of(ErrorType.USER_NOT_FOUND, "존재하지 않는 사용자입니다."));
    }

}