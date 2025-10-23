package com.winten.greenlight.admin.domain.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winten.greenlight.admin.support.dto.AuditDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends AuditDto {
    private Long accountId;
    private String userId;
    private String username;
    @JsonIgnore
    private String password;
    @JsonIgnore
    private String passwordHash;
    private UserRole userRole;
}