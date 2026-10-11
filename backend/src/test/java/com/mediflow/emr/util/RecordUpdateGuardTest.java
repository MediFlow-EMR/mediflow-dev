package com.mediflow.emr.util;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RecordUpdateGuardTest {
    @Test void currentVersionAllowsUpdate() { assertThatCode(() -> RecordUpdateGuard.requireCurrent(1L,1L,2L,2L)).doesNotThrowAnyException(); }
    @Test void staleOrMissingVersionIsConflict() {
        for (Long expected : new Long[]{null,0L,1L}) {
            assertThatThrownBy(() -> RecordUpdateGuard.requireCurrent(1L,1L,2L,expected))
                    .isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException)e).getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
        }
    }
    @Test void PatientCannotChangeDuringUpdate() {
        assertThatThrownBy(() -> RecordUpdateGuard.requireCurrent(1L,2L,2L,2L))
                .isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException)e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
    }
}
