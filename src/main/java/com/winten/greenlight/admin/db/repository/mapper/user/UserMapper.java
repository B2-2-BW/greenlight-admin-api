package com.winten.greenlight.admin.db.repository.mapper.user;

import com.winten.greenlight.admin.domain.user.User;
import com.winten.greenlight.admin.domain.user.AccountStatus;
import com.winten.greenlight.admin.domain.user.UserLoginAttempt;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.domain.user.UserSite;
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
    List<String> findSiteIdsByAccountId(@Param("accountId") Long accountId);
    List<UserSite> findAccessibleSites(@Param("accountId") Long accountId);
    List<UserSite> findAccessibleSitesByAccountIds(@Param("accountIds") List<Long> accountIds);
    void insertSiteAccess(@Param("accountId") Long accountId, @Param("siteId") String siteId);
    void insertSiteAccessBatch(@Param("accountId") Long accountId, @Param("siteIds") List<String> siteIds);
    int deleteSiteAccessByAccountId(@Param("accountId") Long accountId);
    int deleteSiteAccessBySiteId(@Param("siteId") String siteId);
    int reassignHomeSiteIfMissing(@Param("siteId") String siteId);
    UserLoginAttempt findUserLoginAttempt(UserLoginAttempt attempt); // null 별도처리를 하기때문에 Optional 쓰지 않음
    void saveUserLoginAttempt(UserLoginAttempt attempt);
}
