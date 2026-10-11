package com.mediflow.emr.util;

import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import java.util.Objects;

public final class RecordUpdateGuard {
    private RecordUpdateGuard() {}

    public static void requireCurrent(Long patientId, Long requestedPatientId, Long currentVersion, Long expectedVersion) {
        if (!Objects.equals(patientId, requestedPatientId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "기록의 환자는 변경할 수 없습니다");
        }
        if (expectedVersion == null || !Objects.equals(currentVersion, expectedVersion)) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }
}
