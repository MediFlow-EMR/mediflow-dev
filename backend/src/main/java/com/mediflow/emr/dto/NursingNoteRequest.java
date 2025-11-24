package com.mediflow.emr.dto;

import com.mediflow.emr.entity.enums.NoteCategory;
import jakarta.validation.constraints.*;
import lombok.Builder;

/**
 * 간호기록 등록 요청 DTO
 */
@Builder
public record NursingNoteRequest(
        @NotNull(message = "환자 ID는 필수입니다")
        Long patientId,

        @NotBlank(message = "기록 내용은 필수입니다")
        @Size(max = 10000, message = "기록 내용은 10000자 이하여야 합니다")
        String content,

        @Size(max = 10000, message = "Plain text는 10000자 이하여야 합니다")
        String plainText,

        @NotNull(message = "기록 카테고리는 필수입니다")
        NoteCategory category,

        Boolean isImportant
) {
}
