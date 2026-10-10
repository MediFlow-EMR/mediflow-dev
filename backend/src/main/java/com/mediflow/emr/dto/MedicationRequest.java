package com.mediflow.emr.dto;

import com.mediflow.emr.entity.enums.MedicationRoute;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 투약 기록 등록 요청 DTO
 */
@Builder
public record MedicationRequest(
        @NotNull(message = "처방 ID는 필수입니다")
        Long orderId,

        @NotNull(message = "환자 ID는 필수입니다")
        Long patientId,

        @NotBlank(message = "약물명은 필수입니다")
        @Size(max = 200, message = "약물명은 200자 이하여야 합니다")
        String drugName,

        @Size(max = 50, message = "약물 코드는 50자 이하여야 합니다")
        String drugCode,

        @Size(max = 100, message = "용량은 100자 이하여야 합니다")
        String dose,

        @NotNull(message = "투약 경로는 필수입니다")
        MedicationRoute route,

        @Size(max = 100, message = "투약 빈도는 100자 이하여야 합니다")
        String frequency,

        LocalDateTime administeredAt,

        @Size(max = 100, message = "처방 의사명은 100자 이하여야 합니다")
        String orderDoctor,

        @Size(max = 1000, message = "특이사항은 1000자 이하여야 합니다")
        String notes
) {
}
