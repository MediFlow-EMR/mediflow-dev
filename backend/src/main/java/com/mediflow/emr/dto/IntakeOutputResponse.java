package com.mediflow.emr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 섭취배설량 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntakeOutputResponse {

    private Long id;
    private Long patientId;
    private String patientName;
    private Long nurseId;
    private String nurseName;
    
    /** 경구 섭취량 (mL) */
    private Integer intakeOral;
    
    /** 정맥 수액량 (mL) */
    private Integer intakeIv;
    
    /** 섭취 총량 (mL) */
    private Integer intakeTotal;
    
    /** 소변량 (mL) */
    private Integer outputUrine;
    
    /** 배액량 (mL) */
    private Integer outputDrain;
    
    /** 배설 총량 (mL) */
    private Integer outputTotal;
    
    /** 기록 시간 */
    private LocalDateTime recordedAt;
    
    /** 생성 시간 */
    private LocalDateTime createdAt;
    
    /** 현재 사용자가 수정 가능한지 여부 */
    private Boolean canEdit;
}
