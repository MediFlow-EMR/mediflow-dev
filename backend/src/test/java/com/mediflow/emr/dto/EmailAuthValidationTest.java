package com.mediflow.emr.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EmailAuthValidationTest {
    @Test void signupRequiresServerSidePasswordPolicy() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new EmailSignupRequest("valid@example.invalid", "nurse123@"))).isEmpty();
            assertThat(validator.validate(new EmailSignupRequest("valid@example.invalid", "short"))).isNotEmpty();
            assertThat(validator.validate(new EmailSignupRequest("valid@example.invalid", "a1@" + "a".repeat(70)))).isNotEmpty();
        }
    }
    @Test void loginRejectsMalformedEmailAndEmptyPassword() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(new EmailLoginRequest("invalid", ""))).hasSize(2);
        }
    }
}
