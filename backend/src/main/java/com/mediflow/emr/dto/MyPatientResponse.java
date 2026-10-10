package com.mediflow.emr.dto;

import com.mediflow.emr.entity.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 내 담당 환자 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPatientResponse {

    /** 환자 ID */
    private Long patientId;

    /** 차트번호 */
    private String chartNumber;

    /** 환자명 */
    private String name;

    /** 나이 */
    private Integer age;

    /** 성별 */
    private Gender gender;

    /** 주진단명 */
    private String diagnosis;

    /** 알러지 정보 */
    private String allergies;

    /** 트리아지 단계 (응급실만, 1-5) */
    private Integer triageLevel;

    /** 주담당 여부 */
    private Boolean isPrimary;

    /** 최근 바이탈 - 수축기 혈압 */
    private Integer systolicBp;

    /** 최근 바이탈 - 이완기 혈압 */
    private Integer diastolicBp;

    /** 최근 바이탈 - 심박수 */
    private Integer heartRate;

    /** 최근 바이탈 - 체온 */
    private Double bodyTemp;

    /** 최근 바이탈 - 호흡수 */
    private Integer respiratoryRate;

    /** 최근 바이탈 - 산소포화도 */
    private Integer spo2;

    /** 최근 바이탈 측정 시간 */
    private LocalDateTime lastVitalTime;

    /** 부서명 */
    private String departmentName;

    /** 부서 코드 */
    private String departmentCode;

    /** 현재 입원 맥락 */
    private LocalDate admissionDate;
    private String roomNumber;
    private String bedNumber;
    private String attendingPhysician;
    private Boolean fallRisk;
    private String isolationType;
}
