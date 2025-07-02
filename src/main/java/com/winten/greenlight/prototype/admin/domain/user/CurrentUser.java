package com.winten.greenlight.prototype.admin.domain.user;

import lombok.*;

@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@Builder
public class CurrentUser {
    private Long accountId;
    private String userId;
    private UserRole userRole;
}