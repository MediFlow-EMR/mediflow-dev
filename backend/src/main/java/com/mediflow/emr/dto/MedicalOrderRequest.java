package com.mediflow.emr.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalOrderRequest {
    @NotNull @Positive
    private Long patientId;
    @NotBlank @Size(max = 255)
    private String orderType;
    @NotBlank @Size(max = 255)
    private String orderName;
    @Size(max = 255)
    private String orderCode;
    @Size(max = 255)
    private String dose;
    @Size(max = 255)
    private String route;
    @Size(max = 255)
    private String frequency;
    @Size(max = 10000)
    private String instructions;
    private LocalDateTime orderedAt;
    @Size(max = 255)
    private String orderDoctor;
}
