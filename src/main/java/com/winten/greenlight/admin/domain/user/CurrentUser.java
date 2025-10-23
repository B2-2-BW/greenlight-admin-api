package com.winten.greenlight.admin.domain.user;

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