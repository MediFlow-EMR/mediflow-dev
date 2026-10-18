package com.mediflow.emr.service;

import com.mediflow.emr.entity.*;
import com.mediflow.emr.entity.enums.*;
import com.mediflow.emr.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.demo.daily-refresh-enabled", havingValue = "true")
public class DemoDataMaintenanceService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final List<String> ACCOUNTS = List.of("nurseer1@mediflow.com", "nurseicu1@mediflow.com");
    private final UserRepository users;
    private final PatientRepository patients;
    private final ShiftRepository shifts;
    private final AssignmentRepository assignments;
    private final MedicalOrderRepository orders;
    private final VitalSignRepository vitals;
    private final IntakeOutputRepository intakeOutputs;
    private final MedicationRepository medications;
    private final NursingNoteRepository notes;
    private final TestResultRepository tests;
    private final DemoRefreshStateRepository refreshStates;
    private final DemoDrugCatalog catalog;

    @Transactional
    public void refreshAt(LocalDateTime now) {
        // Public demo accounts also serialize refreshes across application instances.
        List<User> demoNurses = ACCOUNTS.stream().map(users::findByEmailForUpdate)
                .flatMap(Optional::stream)
                .filter(user -> user.getProvider() == Provider.LOCAL && user.getRole() == Role.NURSE)
                .filter(user -> user.getDepartment() != null)
                .toList();
        if (demoNurses.isEmpty()) return;
        LocalDate today = now.toLocalDate();
        List<Shift> window = new ArrayList<>();
        for (LocalDate date : List.of(today, today.plusDays(1))) {
            for (ShiftType type : ShiftType.values()) window.add(ensureShift(date, type));
        }
        for (User nurse : demoNurses) {
            List<Patient> admitted = patients.findByDepartmentIdAndIsAdmitted(nurse.getDepartment().getId(), true)
                    .stream().sorted(Comparator.comparing(Patient::getId)).toList();
            for (Shift shift : window) {
                List<Assignment> existing = assignments.findByNurseIdAndShiftId(nurse.getId(), shift.getId());
                if (existing.isEmpty()) {
                    // Keep the original demo patient's selection stable across dates.
                    List<Patient> selected = assignments.findByNurseId(nurse.getId()).stream()
                            .map(Assignment::getPatient).distinct()
                            .filter(p -> admitted.stream().anyMatch(a -> a.getId().equals(p.getId())))
                            .sorted(Comparator.comparing(Patient::getId)).limit(4).toList();
                    if (selected.isEmpty()) selected = admitted.stream().limit(4).toList();
                    existing = assignments.saveAll(selected.stream().map(patient -> Assignment.builder()
                            .nurse(nurse).patient(patient).shift(shift).assignedDate(shift.getDate())
                            .isPrimary(false).build()).toList());
                }
                LocalDateTime start = shift.getDate().atTime(shift.getStartTime());
                if (!shift.getDate().equals(today) || start.isAfter(now)) continue;
                String key = nurse.getEmail() + ":" + today + ":" + shift.getType();
                if (refreshStates.existsById(key) || existing.isEmpty()) continue;
                for (Patient patient : admitted) seedRecords(nurse, patient, shift, start);
                refreshStates.save(new DemoRefreshState(key, now));
                log.info("Demo records refreshed: date={}, shift={}, patients={}", today, shift.getType(), admitted.size());
            }
        }
    }

    private Shift ensureShift(LocalDate date, ShiftType type) {
        return shifts.findByDateAndType(date, type).orElseGet(() -> shifts.save(Shift.builder()
                .date(date).type(type).startTime(startOf(type)).endTime(endOf(type)).build()));
    }

    private LocalTime startOf(ShiftType type) {
        return switch (type) { case NIGHT -> LocalTime.MIDNIGHT; case DAY -> LocalTime.of(8, 0); case EVENING -> LocalTime.of(16, 0); };
    }

    private LocalTime endOf(ShiftType type) {
        return switch (type) { case NIGHT -> LocalTime.of(8, 0); case DAY -> LocalTime.of(16, 0); case EVENING -> LocalTime.MIDNIGHT; };
    }

    private void seedRecords(User nurse, Patient patient, Shift shift, LocalDateTime at) {
        String label = "[시연 자동 생성 " + shift.getDate() + " " + shift.getType() + "]";
        var previousVitals = vitals.findByPatientIdOrderByMeasuredAtDesc(patient.getId());
        VitalSign v = previousVitals.isEmpty() ? null : previousVitals.getFirst();
        vitals.save(VitalSign.builder().patient(patient).nurse(nurse).measuredAt(at)
                .systolicBp(v == null ? 118 : v.getSystolicBp()).diastolicBp(v == null ? 76 : v.getDiastolicBp())
                .heartRate(v == null ? 78 : v.getHeartRate()).bodyTemp(v == null ? 36.7 : v.getBodyTemp())
                .respiratoryRate(v == null ? 18 : v.getRespiratoryRate()).spo2(v == null ? 98 : v.getSpo2()).build());
        var previousIo = intakeOutputs.findByPatientIdOrderByRecordedAtDesc(patient.getId());
        IntakeOutput io = previousIo.isEmpty() ? null : previousIo.getFirst();
        intakeOutputs.save(IntakeOutput.builder().patient(patient).nurse(nurse).recordedAt(at)
                .intakeOral(io == null ? 200 : io.getIntakeOral()).intakeIv(io == null ? 500 : io.getIntakeIv())
                .outputUrine(io == null ? 400 : io.getOutputUrine()).outputDrain(io == null ? 0 : io.getOutputDrain()).build());
        var previousTests = tests.findByPatientIdOrderByResultDateDesc(patient.getId());
        if (!previousTests.isEmpty()) {
            TestResult t = previousTests.getFirst();
            tests.save(TestResult.builder().patient(patient).testType(t.getTestType()).testName(t.getTestName())
                    .resultValue(t.getResultValue()).referenceRange(t.getReferenceRange()).isAbnormal(t.getIsAbnormal())
                    .testDate(shift.getDate()).resultDate(at).status(TestStatus.COMPLETED).build());
        }
        var templates = orders.findByPatientIdOrderByOrderedAtDesc(patient.getId()).stream()
                .filter(o -> "MEDICATION".equals(o.getOrderType()))
                .filter(o -> o.getNotes() == null || !o.getNotes().startsWith("[시연 자동 생성 "))
                .filter(o -> catalog.find(o.getOrderCode()).filter(p -> p.accepts(o.getOrderName())
                        && p.route().equals(o.getRoute())).isPresent())
                .sorted(Comparator.comparing(MedicalOrder::getId)).toList();
        if (!templates.isEmpty()) {
            MedicalOrder template = templates.getFirst();
            MedicalOrder done = orders.save(copyOrder(template, patient, at, label, "COMPLETED"));
            done.setCompletedAt(at);
            done.setCompletedBy(nurse.getName());
            medications.save(Medication.builder().patient(patient).nurse(nurse).medicalOrder(done)
                    .drugName(done.getOrderName()).drugCode(done.getOrderCode()).dose(done.getDose())
                    .route(MedicationRoute.valueOf(done.getRoute())).frequency(done.getFrequency())
                    .administeredAt(at).orderDoctor(done.getOrderDoctor()).administrationNotes(label).build());
            orders.save(copyOrder(templates.size() > 1 ? templates.get(1) : template, patient, at, label, "PENDING"));
        }
        String plain = label + " 가상 환자 상태 확인. 활력징후·섭취배설·검사 및 처방 수행 화면 확인용 기록.";
        notes.save(NursingNote.builder().patient(patient).nurse(nurse).category(NoteCategory.OBSERVATION)
                .content("<p>" + plain + "</p>").plainText(plain).isImportant(false).aiSuggested(false).build());
    }

    private MedicalOrder copyOrder(MedicalOrder source, Patient patient, LocalDateTime at, String label, String status) {
        return MedicalOrder.builder().patient(patient).orderType(source.getOrderType()).orderName(source.getOrderName())
                .orderCode(source.getOrderCode()).dose(source.getDose()).route(source.getRoute()).frequency(source.getFrequency())
                .instructions(source.getInstructions()).status(status).orderedAt(at).orderDoctor(source.getOrderDoctor())
                .notes(label).build();
    }
}
