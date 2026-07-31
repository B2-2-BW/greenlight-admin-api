package com.winten.greenlight.admin.api.controller.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashSet;
import java.util.List;

@Data
public class UserBulkActionRequest {
    @NotEmpty
    @Size(max = 100)
    private List<@NotBlank String> userIds;

    @NotNull
    private UserBulkAction action;

    @NotBlank
    @Size(max = 1000)
    private String reason;

    @AssertTrue(message = "사용자 ID는 중복될 수 없습니다.")
    public boolean isUserIdsUnique() {
        return userIds == null || new HashSet<>(userIds).size() == userIds.size();
    }
}
