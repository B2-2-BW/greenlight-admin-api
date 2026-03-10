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
    private String userSiteId;

    public static CurrentUser guest() {
        return CurrentUser.builder().userRole(UserRole.GUEST).build();
    }
}