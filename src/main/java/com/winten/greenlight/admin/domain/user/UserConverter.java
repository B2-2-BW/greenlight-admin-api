package com.winten.greenlight.admin.domain.user;

import com.winten.greenlight.admin.api.controller.user.*;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConverter {
    User toDto(UserSigninRequest userSigninRequest);
    User toDto(UserUpdateRequest userUpdateRequest);
    LoginInfo toDto(UserLoginRequest userLoginRequest);
    UserResponse toResponse(User user);
    UserLoginResponse toResponse(UserToken token);
}