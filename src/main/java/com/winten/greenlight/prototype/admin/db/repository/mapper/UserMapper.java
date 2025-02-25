package com.winten.greenlight.prototype.admin.db.repository.mapper;

import com.winten.greenlight.prototype.admin.domain.user.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface UserMapper {
    Optional<User> findOne(User user);
    void save(User user);
}