package com.winten.greenlight.admin.api.controller.room;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoomUpdateRequestTest {

    @Test
    void reasonIsOptional() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var violations = validatorFactory.getValidator().validate(new RoomUpdateRequest());

            assertThat(violations)
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .doesNotContain("reason");
        }
    }

    @Test
    void reasonIsLimitedToOneThousandCharacters() {
        var request = new RoomUpdateRequest();
        request.setReason("a".repeat(1001));

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var violations = validatorFactory.getValidator().validate(request);

            assertThat(violations)
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("reason");
        }
    }
}
