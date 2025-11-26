package com.mediflow.emr.dto.handover;

import com.mediflow.emr.entity.Handover;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 인수인계 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HandoverResponse {

    private Long handoverId;
    private String departmentName;
    private String fromShiftType;
    private String toShiftType;
    private String aiSummary;
    private String additionalNotes;
    private String createdByName;
    private String handoverDate;
    private LocalDateTime createdAt;

    public static HandoverResponse from(Handover handover) {
        return HandoverResponse.builder()
                .handoverId(handover.getId())
                .departmentName(handover.getDepartment().getName())
                .fromShiftType(handover.getFromShift().getType().name())
                .toShiftType(handover.getToShift().getType().name())
                .aiSummary(handover.getAiSummary())
                .additionalNotes(handover.getAdditionalNotes())
                .createdByName(handover.getCreatedBy().getName())
                .handoverDate(handover.getCreatedAt().toLocalDate().toString())
                .createdAt(handover.getCreatedAt())
                .build();
    }
}

