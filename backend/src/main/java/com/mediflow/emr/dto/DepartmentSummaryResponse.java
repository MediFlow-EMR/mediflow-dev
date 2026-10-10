package com.mediflow.emr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부서 요약 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentSummaryResponse {
    private Long departmentId;
    private Long shiftId;
    private Long nextShiftId;

    /** 부서명 */
    private String departmentName;

    /** 부서 코드 */
    private String departmentCode;

    /** 총 환자 수 */
    private Long totalPatients;

    /** 내 담당 환자 수 */
    private Long myPatients;

    /** 간호사명 */
    private String nurseName;

    /** 근무조 유형 */
    private String shiftType;
}
