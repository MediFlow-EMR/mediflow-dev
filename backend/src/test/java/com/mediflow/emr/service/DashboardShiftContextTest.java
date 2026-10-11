package com.mediflow.emr.service;

import com.mediflow.emr.entity.*;
import com.mediflow.emr.entity.enums.ShiftType;
import com.mediflow.emr.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardShiftContextTest {
    @Mock UserRepository users;
    @Mock AssignmentRepository assignments;
    @Mock VitalSignRepository vitals;
    @Mock PatientRepository patients;
    @Mock ShiftRepository shifts;
    @InjectMocks DashboardService service;
    final LocalDate date = LocalDate.of(2026, 10, 9);

    void nurse() {
        when(users.findById(1L)).thenReturn(Optional.of(User.builder().id(1L)
            .department(DepartmentEntity.builder().id(2L).code("er").name("응급실").build()).build()));
    }

    @Test void selectedDateAndShiftRetrieveTheirAssignments() {
        nurse();
        when(shifts.findByDateAndType(date, ShiftType.DAY)).thenReturn(Optional.of(
            Shift.builder().id(9L).date(date).type(ShiftType.DAY).build()));
        when(assignments.findByNurseIdAndShiftId(1L, 9L)).thenReturn(List.of(
            Assignment.builder().patient(Patient.builder().id(10L).name("가상환자").build()).build()));
        assertThat(service.getMyPatients(1L, date, ShiftType.DAY)).extracting("patientId").containsExactly(10L);
        verify(assignments).findByNurseIdAndShiftId(1L, 9L);
    }

    @Test void absentSelectedShiftReturnsEmptyInsteadOfTodayAssignments() {
        nurse();
        assertThat(service.getMyPatients(1L, date, ShiftType.NIGHT)).isEmpty();
        verifyNoInteractions(assignments);
    }

    @Test void eveningNextShiftUsesFollowingDateAtMidnight() {
        nurse();
        when(shifts.findByDateAndType(date, ShiftType.EVENING)).thenReturn(Optional.of(
            Shift.builder().id(9L).startTime(LocalTime.of(16,0)).build()));
        when(shifts.findByDateAndType(date, ShiftType.NIGHT)).thenReturn(Optional.of(
            Shift.builder().id(10L).startTime(LocalTime.MIDNIGHT).build()));
        when(shifts.findByDateAndType(date.plusDays(1), ShiftType.NIGHT)).thenReturn(Optional.of(
            Shift.builder().id(11L).startTime(LocalTime.MIDNIGHT).build()));
        var summary = service.getDepartmentSummary(1L, date, ShiftType.EVENING);
        assertThat(summary.getShiftId()).isEqualTo(9L);
        assertThat(summary.getNextShiftId()).isEqualTo(11L);
        assertThat(summary.getShiftType()).isEqualTo("Evening");
    }

    @Test void currentShiftBoundariesIncludeStartAndExcludeEnd() {
        assertThat(DashboardService.shiftTypeAt(LocalTime.MIDNIGHT)).isEqualTo(ShiftType.NIGHT);
        assertThat(DashboardService.shiftTypeAt(LocalTime.of(7,59))).isEqualTo(ShiftType.NIGHT);
        assertThat(DashboardService.shiftTypeAt(LocalTime.of(8,0))).isEqualTo(ShiftType.DAY);
        assertThat(DashboardService.shiftTypeAt(LocalTime.of(16,0))).isEqualTo(ShiftType.EVENING);
    }
}
