package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.user.AccountStatus;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserUpdateRequestTest {

    @Test
    void statusChangeReasonIsRequired() {
        var request = new UserUpdateRequest();
        request.setAccountStatus(AccountStatus.ACTIVE);
        request.setReason(" ");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(request);

            assertThat(violations)
                    .anySatisfy(violation ->
                            assertThat(violation.getPropertyPath().toString()).isEqualTo("reason"));
        }
    }
}
