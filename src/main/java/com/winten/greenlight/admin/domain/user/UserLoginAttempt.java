package com.winten.greenlight.admin.domain.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winten.greenlight.admin.support.dto.AuditDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UserLoginAttempt extends AuditDto {
    private String userId;
    @JsonIgnore
    private Integer passwordErrorCount;
}