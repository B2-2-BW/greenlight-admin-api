package com.winten.greenlight.admin.api.controller.user;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserBulkActionRequestTest {

    @Test
    void rejectsDuplicateUserIdsAndBlankReason() {
        var request = new UserBulkActionRequest();
        request.setUserIds(List.of("user-1", "user-1"));
        request.setAction(UserBulkAction.APPROVE);
        request.setReason(" ");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(request);
            assertThat(violations)
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("userIdsUnique", "reason");
        }
    }
}
