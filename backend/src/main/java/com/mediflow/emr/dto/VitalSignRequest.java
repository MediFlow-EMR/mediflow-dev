package com.mediflow.emr.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 바이탈 사인 등록 요청 DTO
 */
@Builder
public record VitalSignRequest(
        @NotNull(message = "환자 ID는 필수입니다")
        Long patientId,

        @Min(value = 50, message = "수축기 혈압은 50 이상이어야 합니다")
        @Max(value = 250, message = "수축기 혈압은 250 이하여야 합니다")
        Integer systolicBp,

        @Min(value = 30, message = "이완기 혈압은 30 이상이어야 합니다")
        @Max(value = 150, message = "이완기 혈압은 150 이하여야 합니다")
        Integer diastolicBp,

        @Min(value = 30, message = "심박수는 30 이상이어야 합니다")
        @Max(value = 250, message = "심박수는 250 이하여야 합니다")
        Integer heartRate,

        @DecimalMin(value = "35.0", message = "체온은 35.0°C 이상이어야 합니다")
        @DecimalMax(value = "42.0", message = "체온은 42.0°C 이하여야 합니다")
        @Digits(integer = 2, fraction = 1, message = "체온은 소수점 한자리까지만 입력 가능합니다")
        Double bodyTemp,

        @Min(value = 5, message = "호흡수는 5 이상이어야 합니다")
        @Max(value = 60, message = "호흡수는 60 이하여야 합니다")
        Integer respiratoryRate,

        @Min(value = 70, message = "산소포화도는 70% 이상이어야 합니다")
        @Max(value = 100, message = "산소포화도는 100% 이하여야 합니다")
        Integer spo2,

        LocalDateTime measuredAt,

        @PositiveOrZero Long version
) {
    @AssertTrue(message = "측정값을 하나 이상 입력해주세요")
    public boolean isMeasurementPresent() {
        return systolicBp != null || diastolicBp != null || heartRate != null || bodyTemp != null || respiratoryRate != null || spo2 != null;
    }
}
