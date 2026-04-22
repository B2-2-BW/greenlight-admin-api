package com.winten.greenlight.admin.domain.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winten.greenlight.admin.support.dto.AuditDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class User extends AuditDto {
    private Long accountId;
    private String userId;
    private String siteId;
    private String siteName;
    private String username;
    private String userEmail;
    private Boolean passwordResetRequired;
    private AccountStatus accountStatus;
    @JsonIgnore
    private LocalDateTime passwordChangedAt;
    private String phoneNumber;
    @JsonIgnore
    private String password;
    @JsonIgnore
    private String passwordHash;
    private UserRole userRole;
    private Boolean autoLogin;
}