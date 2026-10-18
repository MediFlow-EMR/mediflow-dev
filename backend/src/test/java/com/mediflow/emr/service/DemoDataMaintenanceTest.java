package com.mediflow.emr.service;

import com.mediflow.emr.entity.*;
import com.mediflow.emr.entity.enums.*;
import com.mediflow.emr.repository.*;
import com.mediflow.emr.scheduler.DemoDataScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(properties = {
    "app.demo.daily-refresh-enabled=true",
    "spring.datasource.url=jdbc:h2:mem:demo-maintenance;MODE=MariaDB;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Transactional
class DemoDataMaintenanceTest {
    @MockitoBean DemoDataScheduler scheduler;
    @Autowired DemoDataMaintenanceService service;
    @Autowired DepartmentRepository departments;
    @Autowired UserRepository users;
    @Autowired PatientRepository patients;
    @Autowired ShiftRepository shifts;
    @Autowired AssignmentRepository assignments;
    @Autowired MedicalOrderRepository orders;
    @Autowired NursingNoteRepository notes;
    @Autowired VitalSignRepository vitals;
    @Autowired IntakeOutputRepository io;
    @Autowired MedicationRepository medications;
    @Autowired TestResultRepository tests;
    @Autowired DemoRefreshStateRepository states;
    @Autowired DemoDrugCatalog catalog;
    @Autowired DashboardService dashboard;

    @Test void midnightRestartAndLaterShiftsKeepAssignmentsAndFreshRecordsWithoutChangingHistory() {
        var d = departments.save(DepartmentEntity.builder().name("가상 응급실").code("ER").type(Department.EMERGENCY).build());
        var nurse = users.save(User.builder().email("nurseer1@mediflow.com").nickname("데모")
                .name("데모").provider(Provider.LOCAL).providerId("nurse_ER_1").role(Role.NURSE).department(d).build());
        var other = users.save(User.builder().email("ordinary@example.invalid").nickname("일반")
                .provider(Provider.LOCAL).providerId("ordinary").role(Role.NURSE).department(d).build());
        var p = patients.save(Patient.builder().chartNumber("DEMO-REFRESH").name("가상환자").age(40).gender(Gender.F)
                .department(d).isAdmitted(true).admissionDate(LocalDate.of(2026, 10, 1)).build());
        var yesterday = LocalDate.of(2026, 10, 18);
        var historical = shifts.save(Shift.builder().date(yesterday).type(ShiftType.DAY)
                .startTime(LocalTime.of(8,0)).endTime(LocalTime.of(16,0)).build());
        assignments.save(Assignment.builder().nurse(nurse).patient(p).shift(historical).assignedDate(yesterday).build());
        var product = catalog.find("CARD-ASA").orElseThrow();
        var original = orders.save(MedicalOrder.builder().patient(p).orderType("MEDICATION").orderName(product.displayName())
                .orderCode("CARD-ASA").dose("100mg").route(product.route()).frequency("1일 1회")
                .instructions("기존 지시").status("PENDING").orderedAt(yesterday.atTime(9,0)).build());
        tests.save(TestResult.builder().patient(p).testType(TestType.BLOOD).testName("시연 검사")
                .resultValue("가상 결과").referenceRange("시연 참고치").isAbnormal(false).testDate(yesterday)
                .resultDate(yesterday.atTime(9,0)).status(TestStatus.COMPLETED).build());
        // A partial next-day shift must not prevent the other two from being created.
        shifts.save(Shift.builder().date(yesterday.plusDays(1)).type(ShiftType.NIGHT)
                .startTime(LocalTime.MIDNIGHT).endTime(LocalTime.of(8,0)).build());
        var midnight = yesterday.plusDays(1).atStartOfDay();
        service.refreshAt(midnight);
        assertThat(shifts.findByDate(midnight.toLocalDate())).hasSize(3);
        assertThat(shifts.findByDate(midnight.toLocalDate().plusDays(1))).hasSize(3);
        for (var shift : shifts.findByDate(midnight.toLocalDate())) {
            assertThat(assignments.findByNurseIdAndShiftId(nurse.getId(), shift.getId())).hasSize(1);
        }
        assertThat(dashboard.getMyPatients(nurse.getId(), midnight.toLocalDate(), ShiftType.NIGHT)).hasSize(1);
        assertThat(assignments.findByNurseId(other.getId())).isEmpty();
        assertThat(vitals.count()).isEqualTo(1);
        assertThat(io.count()).isEqualTo(1);
        assertThat(medications.count()).isEqualTo(1);
        assertThat(notes.count()).isEqualTo(1);
        assertThat(tests.count()).isEqualTo(2);
        assertThat(orders.count()).isEqualTo(3);
        assertThat(vitals.findAll()).allMatch(v -> !v.getMeasuredAt().isAfter(midnight));
        var done = medications.findAll().getFirst();
        assertThat(done.getMedicalOrder().getStatus()).isEqualTo("COMPLETED");
        assertThat(done.getAdministeredAt()).isEqualTo(midnight);
        assertThat(notes.findAll().getFirst().getPlainText()).startsWith("[시연 자동 생성 ");
        service.refreshAt(midnight.plusMinutes(5));
        assertThat(orders.count()).isEqualTo(3);
        assertThat(states.count()).isEqualTo(1);
        assertThat(assignments.findByNurseId(nurse.getId())).hasSize(7);
        // The pending sample can be completed without the refresh resetting it.
        var pending = orders.findAll().stream().filter(o -> o.getNotes() != null && o.getStatus().equals("PENDING")).findFirst().orElseThrow();
        pending.updateStatus("COMPLETED", nurse.getName());
        service.refreshAt(midnight.plusMinutes(10));
        assertThat(pending.getStatus()).isEqualTo("COMPLETED");
        service.refreshAt(midnight.plusHours(23));
        assertThat(states.count()).isEqualTo(3);
        assertThat(orders.count()).isEqualTo(7);
        var summary = dashboard.getDepartmentSummary(nurse.getId(), midnight.toLocalDate(), ShiftType.EVENING);
        assertThat(summary.getNextShiftId()).isEqualTo(shifts.findByDateAndType(midnight.toLocalDate().plusDays(1),ShiftType.NIGHT).orElseThrow().getId());
        service.refreshAt(midnight.plusDays(3));
        assertThat(dashboard.getMyPatients(nurse.getId(), midnight.toLocalDate().plusDays(3), ShiftType.NIGHT)).hasSize(1);
        assertThat(orders.findById(original.getId()).orElseThrow().getOrderedAt()).isEqualTo(yesterday.atTime(9,0));
        assertThat(orders.findById(original.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
        assertThat(assignments.findByNurseIdAndShiftId(nurse.getId(), historical.getId())).hasSize(1);
    }
    @Test void icuDemoRecoversWithoutErAccountAndDoesNotCreateFutureClinicalRecords() {
        var d = departments.save(DepartmentEntity.builder().name("가상 중환자실").code("ICU").type(Department.ICU).build());
        var nurse = users.save(User.builder().email("nurseicu1@mediflow.com").nickname("ICU 데모")
                .provider(Provider.LOCAL).providerId("nurse_ICU_1").role(Role.NURSE).department(d).build());
        var p = patients.save(Patient.builder().chartNumber("DEMO-ICU").name("가상환자").age(50).gender(Gender.M)
                .department(d).isAdmitted(true).admissionDate(LocalDate.of(2026,10,1)).build());
        var at = LocalDateTime.of(2026,10,19,7,59);
        service.refreshAt(at);
        assertThat(dashboard.getMyPatients(nurse.getId(), at.toLocalDate(), ShiftType.DAY)).hasSize(1);
        assertThat(vitals.count()).isEqualTo(1);
        assertThat(vitals.findAll()).allMatch(v -> !v.getMeasuredAt().isAfter(at));
        assertThat(states.count()).isEqualTo(1);
        service.refreshAt(at.plusMinutes(1));
        assertThat(vitals.count()).isEqualTo(2);
        assertThat(states.count()).isEqualTo(2);
        // A manual edit to the generated note does not cause the same slot to be seeded again.
        var note = notes.findByPatientIdOrderByCreatedAtDesc(p.getId()).getFirst();
        note.update("<p>수정한 내용</p>", "수정한 내용", NoteCategory.EDUCATION, true);
        service.refreshAt(at.plusMinutes(2));
        assertThat(notes.count()).isEqualTo(2);
        assertThat(note.getPlainText()).isEqualTo("수정한 내용");
    }

}
