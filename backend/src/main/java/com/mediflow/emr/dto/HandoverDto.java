package com.mediflow.emr.dto;

import com.mediflow.emr.entity.Handover;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverDto {
    
    private Long id;
    private Long departmentId;
    private String departmentName;
    private Long fromShiftId;
    private String fromShiftType;
    private Long toShiftId;
    private String toShiftType;
    private LocalDate handoverDate;
    private String aiSummary;
    private String additionalNotes;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    
    public static HandoverDto from(Handover handover) {
        return HandoverDto.builder()
                .id(handover.getId())
                .departmentId(handover.getDepartment().getId())
                .departmentName(handover.getDepartment().getName())
                .fromShiftId(handover.getFromShift().getId())
                .fromShiftType(handover.getFromShift().getType().name())
                .toShiftId(handover.getToShift().getId())
                .toShiftType(handover.getToShift().getType().name())
                .handoverDate(handover.getHandoverDate())
                .aiSummary(handover.getAiSummary())
                .additionalNotes(handover.getAdditionalNotes())
                .createdById(handover.getCreatedBy().getId())
                .createdByName(handover.getCreatedBy().getName())
                .createdAt(handover.getCreatedAt())
                .build();
    }
}
