package com.winten.greenlight.admin.db.repository.mapper.user;

import com.winten.greenlight.admin.domain.user.User;
import com.winten.greenlight.admin.domain.user.AccountStatus;
import com.winten.greenlight.admin.domain.user.UserLoginAttempt;
import com.winten.greenlight.admin.domain.user.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    Optional<User> findUserById(@Param("userId") String userId);
    Optional<User> findUserByEmail(@Param("email") String email);
    Optional<User> findUserWithCredential(@Param("userId") String userId);
    List<User> findAllUsers(@Param("siteId") String siteId);
    List<User> findUsersPage(@Param("siteId") String siteId, @Param("query") String query,
                             @Param("status") AccountStatus status,
                             @Param("role") UserRole role,
                             @Param("limit") int limit, @Param("offset") long offset);
    long countUsers(@Param("siteId") String siteId, @Param("query") String query,
                    @Param("status") AccountStatus status,
                    @Param("role") UserRole role);
    List<UserStatusCount> countUsersByStatus(@Param("siteId") String siteId, @Param("query") String query,
                                             @Param("role") UserRole role);
    void saveUser(User user);
    void updateUserStatus(User user);
    int approveUser(User user);
    int updateManagedUser(User user);
    void updateUserProfile(User user);
    void updateUserPassword(User user);
    void updateUserLoginAttempt(UserLoginAttempt user);
    void resetUserPassword(User user);
    int disableUsersBySiteId(@Param("siteId") String siteId);
    UserLoginAttempt findUserLoginAttempt(UserLoginAttempt attempt); // null 별도처리를 하기때문에 Optional 쓰지 않음
    void saveUserLoginAttempt(UserLoginAttempt attempt);
}
