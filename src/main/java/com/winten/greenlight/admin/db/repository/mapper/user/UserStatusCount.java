package com.winten.greenlight.admin.db.repository.mapper.user;

import com.winten.greenlight.admin.domain.user.AccountStatus;
import lombok.Data;

@Data
public class UserStatusCount {
    private AccountStatus accountStatus;
    private long count;
}
