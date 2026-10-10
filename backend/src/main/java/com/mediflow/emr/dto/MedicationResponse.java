package com.mediflow.emr.dto;

import com.mediflow.emr.entity.enums.MedicationRoute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 투약 기록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationResponse {

    private Long id;
    private Long patientId;
    private Long orderId;
    private String patientName;
    private Long nurseId;
    private String nurseName;
    private String drugName;
    private String drugCode;
    private String dose;
    private MedicationRoute route;
    private String frequency;
    private LocalDateTime administeredAt;
    private String orderDoctor;
    private String notes;
    private LocalDateTime createdAt;
    
    /** 현재 사용자가 수정 가능한지 여부 */
    private Boolean canEdit;
}
