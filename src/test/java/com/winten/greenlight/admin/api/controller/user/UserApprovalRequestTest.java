package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.user.UserRole;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserApprovalRequestTest {

    @Test
    void approvalReasonIsRequired() {
        var request = new UserApprovalRequest();
        request.setUsername("승인 사용자");
        request.setUserEmail("approved@example.com");
        request.setSiteIds(java.util.List.of("site-a"));
        request.setUserRole(UserRole.USER);
        request.setReason(" ");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(request);

            assertThat(violations)
                    .anySatisfy(violation ->
                            assertThat(violation.getPropertyPath().toString()).isEqualTo("reason"));
        }
    }
}
