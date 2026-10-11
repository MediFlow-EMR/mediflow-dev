package com.mediflow.emr.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ClinicalRecordValidationTest {
    @Test void emptyMeasurementsAreRejectedButZeroOutputIsValid() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(VitalSignRequest.builder().patientId(1L).build())).isNotEmpty();
            assertThat(validator.validate(IntakeOutputRequest.builder().patientId(1L).build())).isNotEmpty();
            assertThat(validator.validate(IntakeOutputRequest.builder().patientId(1L).outputUrine(0).build())).isEmpty();
            assertThat(validator.validate(VitalSignRequest.builder().patientId(1L).heartRate(80).build())).isEmpty();
        }
    }
    @Test void orderRequestsRejectMissingIdentityAndOversizedStrings() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(MedicalOrderRequest.builder().build())).isNotEmpty();
            assertThat(validator.validate(MedicalOrderRequest.builder().patientId(1L).orderType("LAB").orderName("검사").build())).isEmpty();
            assertThat(validator.validate(MedicalOrderRequest.builder().patientId(1L).orderType("LAB").orderName("검".repeat(256)).build())).isNotEmpty();
        }
    }
    @Test void negativeVersionsAndOutOfRangeMeasurementsAreRejected() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(VitalSignRequest.builder().patientId(1L).heartRate(80).version(-1L).build())).isNotEmpty();
            assertThat(validator.validate(IntakeOutputRequest.builder().patientId(1L).outputUrine(-1).build())).isNotEmpty();
        }
    }
}
