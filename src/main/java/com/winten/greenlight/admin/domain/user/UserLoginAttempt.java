package com.winten.greenlight.admin.domain.user;

import com.winten.greenlight.admin.support.dto.AuditDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UserLoginAttempt extends AuditDto {
    private String loginId;
    private String userId;
    private Integer passwordErrorCount;
}