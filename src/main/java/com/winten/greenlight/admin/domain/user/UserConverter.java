package com.winten.greenlight.admin.domain.user;

import com.winten.greenlight.admin.api.controller.user.UserCreateRequest;
import com.winten.greenlight.admin.api.controller.user.UserLoginRequest;
import com.winten.greenlight.admin.api.controller.user.UserLoginResponse;
import com.winten.greenlight.admin.api.controller.user.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConverter {
    User toDto(UserCreateRequest userCreateRequest);
    User toDto(UserLoginRequest userLoginRequest);
    UserResponse toResponse(User user);
    UserLoginResponse toResponse(UserToken token);
}