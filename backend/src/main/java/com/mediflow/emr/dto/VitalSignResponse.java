package com.mediflow.emr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 바이탈 사인 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalSignResponse {

    private Long id;
    private Long version;
    private Long patientId;
    private String patientName;
    private Long nurseId;
    private String nurseName;
    private Integer systolicBp;
    private Integer diastolicBp;
    private Integer heartRate;
    private Double bodyTemp;
    private Integer respiratoryRate;
    private Integer spo2;
    private LocalDateTime measuredAt;
    private LocalDateTime createdAt;
    
    /** 현재 사용자가 수정 가능한지 여부 */
    private Boolean canEdit;
}
