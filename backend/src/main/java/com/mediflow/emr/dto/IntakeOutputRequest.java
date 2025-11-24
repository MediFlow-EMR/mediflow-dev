package com.mediflow.emr.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 섭취배설량 등록 요청 DTO
 */
@Builder
public record IntakeOutputRequest(
        @NotNull(message = "환자 ID는 필수입니다")
        Long patientId,

        @Min(value = 0, message = "경구 섭취량은 0 이상이어야 합니다")
        @Max(value = 5000, message = "경구 섭취량은 5000mL 이하여야 합니다")
        Integer intakeOral,

        @Min(value = 0, message = "정맥 수액량은 0 이상이어야 합니다")
        @Max(value = 5000, message = "정맥 수액량은 5000mL 이하여야 합니다")
        Integer intakeIv,

        @Min(value = 0, message = "소변량은 0 이상이어야 합니다")
        @Max(value = 5000, message = "소변량은 5000mL 이하여야 합니다")
        Integer outputUrine,

        @Min(value = 0, message = "배액량은 0 이상이어야 합니다")
        @Max(value = 5000, message = "배액량은 5000mL 이하여야 합니다")
        Integer outputDrain,

        LocalDateTime recordedAt
) {
}
