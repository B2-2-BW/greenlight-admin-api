package com.winten.greenlight.admin.domain.user;

import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class UserPage {
    List<User> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
    Map<AccountStatus, Long> statusCounts;
}
