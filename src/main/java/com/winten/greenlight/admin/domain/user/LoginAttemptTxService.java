package com.winten.greenlight.admin.domain.user;

import com.winten.greenlight.admin.db.repository.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAttemptTxService {
    private final UserMapper userMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveNewLoginAttempt(UserLoginAttempt userLoginAttempt) {
        userMapper.saveUserLoginAttempt(userLoginAttempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePasswordErrorCountById(String loginId, int count) {
        var user = UserLoginAttempt.builder()
                .loginId(loginId)
                .passwordErrorCount(count)
                .build();
        userMapper.updateUserLoginAttempt(user);
    }

}