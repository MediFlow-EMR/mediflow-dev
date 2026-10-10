package com.mediflow.emr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** AI 초안과 의료인이 확인한 최종본을 분리해 보존한다. */
public record HandoverSaveRequest(
        @NotNull Long departmentId,
        @NotNull Long fromShiftId,
        @NotNull Long toShiftId,
        @NotBlank String aiDraft,
        @NotBlank String finalSummary
) {}
