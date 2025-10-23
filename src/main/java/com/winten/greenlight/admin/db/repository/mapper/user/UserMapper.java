package com.winten.greenlight.admin.db.repository.mapper.user;

import com.winten.greenlight.admin.domain.user.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface UserMapper {
    Optional<User> findOne(String userId);
    Optional<User> findUserWithCredential(String userId);
    void save(User user);
    Optional<User> findUserAccountIdByApiKey(String apiKey);
}