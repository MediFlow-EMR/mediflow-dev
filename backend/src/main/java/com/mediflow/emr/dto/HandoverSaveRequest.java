package com.mediflow.emr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Draft and approved handovers share one payload; AI provenance stays separate. */
public record HandoverSaveRequest(
        @NotNull Long departmentId,
        @NotNull Long fromShiftId,
        @NotNull Long toShiftId,
        String aiDraft,
        @NotBlank String finalSummary,
        String sourceType,
        String status,
        Long version
) {}
