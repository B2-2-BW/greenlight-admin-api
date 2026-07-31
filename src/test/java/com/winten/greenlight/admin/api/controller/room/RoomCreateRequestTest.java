package com.winten.greenlight.admin.api.controller.room;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoomCreateRequestTest {
    @Test
    void enabledIsRequiredForRoomCreation() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var violations = validatorFactory.getValidator().validate(new RoomCreateRequest());

            assertThat(violations)
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("enabled");
        }
    }
}
