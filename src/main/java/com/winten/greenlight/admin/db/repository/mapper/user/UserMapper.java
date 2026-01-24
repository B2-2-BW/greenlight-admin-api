package com.winten.greenlight.admin.db.repository.mapper.user;

import com.winten.greenlight.admin.domain.user.User;
import com.winten.greenlight.admin.domain.user.UserLoginAttempt;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface UserMapper {
    Optional<User> findUserById(String userId);
    Optional<User> findUserByEmail(String email);
    Optional<User> findUserWithCredential(String userId);
    void saveUser(User user);
    void updateUserLoginAttempt(UserLoginAttempt user);
    void resetUserPassword(User user);
    UserLoginAttempt findUserLoginAttempt(UserLoginAttempt attempt); // null 별도처리를 하기때문에 Optional 쓰지 않음
    void saveUserLoginAttempt(UserLoginAttempt attempt);
}