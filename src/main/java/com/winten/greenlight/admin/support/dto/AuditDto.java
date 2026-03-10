package com.winten.greenlight.admin.support.dto;

import com.winten.greenlight.admin.domain.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public abstract class AuditDto {
    protected String userSiteId;
    protected UserRole userRole;
    protected String createdBy;
    protected LocalDateTime createdAt;
    protected String createdIp;
    protected String updatedBy;
    protected LocalDateTime updatedAt;
    protected String updatedIp;
}