package com.winten.greenlight.prototype.admin.domain.user;

import com.winten.greenlight.prototype.admin.support.dto.AuditDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends AuditDto {
    private long userId;
    private String username;
    private String userNickname;
    private String password;
    private String passwordHash;
    private String passcode;
}