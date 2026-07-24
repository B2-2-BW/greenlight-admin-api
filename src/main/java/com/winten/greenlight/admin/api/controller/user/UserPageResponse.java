package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.user.AccountStatus;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class UserPageResponse {
    List<UserResponse> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
    Map<AccountStatus, Long> statusCounts;
}
