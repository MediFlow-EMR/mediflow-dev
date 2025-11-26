package com.mediflow.emr.dto.handover;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 인수인계 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HandoverRequest {

    @NotNull(message = "부서 ID는 필수입니다")
    private Long departmentId;

    @NotNull(message = "인계 근무조 ID는 필수입니다")
    private Long fromShiftId;

    @NotNull(message = "인수 근무조 ID는 필수입니다")
    private Long toShiftId;

    private String additionalNotes;  // 추가 메모 (선택)
}

