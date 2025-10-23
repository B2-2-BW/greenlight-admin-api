package com.winten.greenlight.admin.support.dto;

import com.winten.greenlight.admin.domain.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthDto {
    private Long accountId;
    private String userId;
    private String username;
    private UserRole userRole;
}