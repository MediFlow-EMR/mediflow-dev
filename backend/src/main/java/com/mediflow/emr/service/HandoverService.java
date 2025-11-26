package com.mediflow.emr.service;

import com.mediflow.emr.entity.*;
import com.mediflow.emr.entity.enums.Gender;
import com.mediflow.emr.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandoverService {

    private final PatientRepository patientRepository;
    private final NursingNoteRepository nursingNoteRepository;
    private final VitalSignRepository vitalSignRepository;
    private final MedicationRepository medicationRepository;
    private final IntakeOutputRepository intakeOutputRepository;
    private final TestResultRepository testResultRepository;
    private final DepartmentRepository departmentRepository;
    private final ShiftRepository shiftRepository;
    private final GeminiService geminiService;
    private final HandoverRepository handoverRepository;
    private final AssignmentRepository assignmentRepository;

    @Transactional(readOnly = true)
    public String generateAiSummary(Long nurseId, Long fromShiftId) {
        log.info("AI 인수인계 요약 생성 - nurseId: {}, fromShiftId: {}", nurseId, fromShiftId);

        Shift fromShift = shiftRepository.findById(fromShiftId)
            .orElseThrow(() -> new IllegalArgumentException("근무조를 찾을 수 없습니다."));

        LocalDate today = LocalDate.now();

        // 오늘 날짜 + 해당 근무조에 배정된 환자만 조회
        List<Assignment> allAssignments = assignmentRepository.findByNurseIdAndShiftId(nurseId, fromShiftId);
        log.info("전체 배정 수: {}, 조회 조건 - nurseId: {}, shiftId: {}", allAssignments.size(), nurseId, fromShiftId);
        
        List<Assignment> assignments = allAssignments.stream()
            .filter(a -> a.getAssignedDate().equals(today))
            .collect(Collectors.toList());
        
        log.info("오늘({}) 배정 수: {}", today, assignments.size());
        
        if (assignments.isEmpty()) {
            log.warn("오늘({}) 근무조({})에 배정된 환자가 없습니다 - nurseId: {}", today, fromShiftId, nurseId);
            return "현재 근무조에 배정된 환자가 없습니다.";
        }

        List<Patient> patients = assignments.stream()
            .map(Assignment::getPatient)
            .distinct()
            .collect(Collectors.toList());

        log.info("배정된 환자 수: {}", patients.size());
        patients.forEach(p -> log.info("환자 ID: {}, 이름: {}, 차트번호: {}", p.getId(), p.getName(), p.getChartNumber()));

        // 부서 정보는 첫 번째 환자의 부서로 설정
        DepartmentEntity department = patients.get(0).getDepartment();

        final LocalDateTime shiftStart = LocalDateTime.of(today, fromShift.getStartTime());
        LocalDateTime tempEnd = LocalDateTime.of(today, fromShift.getEndTime());
        
        if (fromShift.getEndTime().isBefore(fromShift.getStartTime())) {
            tempEnd = tempEnd.plusDays(1);
        }
        final LocalDateTime shiftEnd = tempEnd;

        List<PatientData> patientDataList = patients.stream()
            .map(patient -> collectPatientData(patient, shiftStart, shiftEnd))
            .sorted((a, b) -> Boolean.compare(b.important, a.important))
            .collect(Collectors.toList());

        log.info("수집된 환자 데이터 수: {}", patientDataList.size());

        String prompt = buildPrompt(department, fromShift, patientDataList);
        log.info("생성된 프롬프트 길이: {}", prompt.length());
        return geminiService.generateContent(prompt);
    }

    private PatientData collectPatientData(Patient patient, LocalDateTime start, LocalDateTime end) {
        PatientData data = new PatientData();
        data.patient = patient;

        data.notes = nursingNoteRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
            .stream()
            .filter(n -> !n.getCreatedAt().isBefore(start) && !n.getCreatedAt().isAfter(end))
            .collect(Collectors.toList());

        data.important = data.notes.stream().anyMatch(NursingNote::getIsImportant);

        data.vitals = vitalSignRepository.findByPatientIdAndMeasuredAtBetween(patient.getId(), start, end);
        data.important = data.important || data.vitals.stream().anyMatch(this::isAbnormal);

        data.medications = medicationRepository.findByPatientIdAndAdministeredAtBetween(patient.getId(), start, end);
        data.intakeOutputs = intakeOutputRepository.findByPatientIdAndRecordedAtBetween(patient.getId(), start, end);
        data.important = data.important || data.intakeOutputs.stream().anyMatch(this::hasImbalance);

        data.testResults = testResultRepository.findByPatientIdOrderByResultDateDesc(patient.getId())
            .stream()
            .filter(r -> r.getResultDate().toLocalDate().equals(LocalDate.now()))
            .collect(Collectors.toList());

        return data;
    }

    private boolean isAbnormal(VitalSign v) {
        return (v.getSystolicBp() != null && (v.getSystolicBp() > 140 || v.getSystolicBp() < 90)) ||
               (v.getDiastolicBp() != null && (v.getDiastolicBp() > 90 || v.getDiastolicBp() < 60)) ||
               (v.getHeartRate() != null && (v.getHeartRate() > 100 || v.getHeartRate() < 60)) ||
               (v.getBodyTemp() != null && (v.getBodyTemp() > 37.5 || v.getBodyTemp() < 36.0)) ||
               (v.getSpo2() != null && v.getSpo2() < 95);
    }

    private boolean hasImbalance(IntakeOutput io) {
        return Math.abs(io.getIntakeTotal() - io.getOutputTotal()) > 500;
    }

    private String buildPrompt(DepartmentEntity dept, Shift shift, List<PatientData> dataList) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음은 [").append(dept.getName()).append("] [근무조: ")
          .append(shift.getType()).append("]의 인수인계 정보입니다.\n\n");

        for (PatientData data : dataList) {
            Patient p = data.patient;
            sb.append("[환자 - ").append(p.getName())
              .append(" (").append(p.getChartNumber()).append(", ")
              .append(p.getAge()).append("세/")
              .append(p.getGender() == Gender.M ? "남" : "여")
              .append(")]\n");

            if (!data.notes.isEmpty()) {
                sb.append("- 간호기록:\n");
                data.notes.forEach(n -> sb.append("  * ").append(n.getCreatedAt().toLocalTime())
                    .append(" ").append(n.getPlainText()).append("\n"));
            }

            if (!data.vitals.isEmpty()) {
                VitalSign v = data.vitals.get(0);
                sb.append("- 바이탈: ");
                if (v.getSystolicBp() != null) sb.append("BP ").append(v.getSystolicBp()).append("/").append(v.getDiastolicBp()).append(", ");
                if (v.getHeartRate() != null) sb.append("HR ").append(v.getHeartRate()).append(", ");
                if (v.getBodyTemp() != null) sb.append("Temp ").append(v.getBodyTemp()).append(", ");
                if (v.getSpo2() != null) sb.append("SpO2 ").append(v.getSpo2()).append("%");
                sb.append("\n");
            }

            if (!data.testResults.isEmpty()) {
                sb.append("- 검사결과: ");
                data.testResults.forEach(t -> sb.append(t.getTestType()).append(" ").append(t.getTestName()).append(", "));
                sb.append("\n");
            }

            if (!data.medications.isEmpty()) {
                sb.append("- 투약: ");
                Map<String, Long> counts = data.medications.stream()
                    .collect(Collectors.groupingBy(Medication::getDrugName, Collectors.counting()));
                counts.forEach((drug, count) -> sb.append(drug).append(" (").append(count).append("회), "));
                sb.append("\n");
            }

            if (!data.intakeOutputs.isEmpty()) {
                int intake = data.intakeOutputs.stream().mapToInt(IntakeOutput::getIntakeTotal).sum();
                int output = data.intakeOutputs.stream().mapToInt(IntakeOutput::getOutputTotal).sum();
                sb.append("- I/O: 섭취 ").append(intake).append("mL, 배설 ").append(output).append("mL\n");
            }

            sb.append("\n");
        }

        sb.append("\n각 환자별로 다음 형식으로 인수인계문을 작성해줘:\n\n");
        sb.append("[환자명 (차트번호, 나이/성별)]\n");
        sb.append("- 주요 변화: 특이사항 및 상태 변화\n");
        sb.append("- 수행한 처치: 투약, 검사 등\n");
        sb.append("- 지속 관찰 사항: 다음 근무조에서 주의할 점\n\n");
        sb.append("환자당 3-5문장, 중요한 환자는 더 자세히 작성. 간결하고 명확하게.");

        return sb.toString();
    }

    private static class PatientData {
        Patient patient;
        List<NursingNote> notes = new ArrayList<>();
        List<VitalSign> vitals = new ArrayList<>();
        List<Medication> medications = new ArrayList<>();
        List<IntakeOutput> intakeOutputs = new ArrayList<>();
        List<TestResult> testResults = new ArrayList<>();
        boolean important = false;

        boolean hasData() {
            return !notes.isEmpty() || !vitals.isEmpty() || !medications.isEmpty() || 
                   !intakeOutputs.isEmpty() || !testResults.isEmpty();
        }
    }

    @Transactional
    public void saveHandover(Long departmentId, Long fromShiftId, Long toShiftId, String aiSummary, User user) {
        DepartmentEntity department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다."));
        
        Shift fromShift = shiftRepository.findById(fromShiftId)
            .orElseThrow(() -> new IllegalArgumentException("인계 근무조를 찾을 수 없습니다."));
        
        Shift toShift = shiftRepository.findById(toShiftId)
            .orElseThrow(() -> new IllegalArgumentException("인수 근무조를 찾을 수 없습니다."));

        Handover handover = Handover.builder()
            .department(department)
            .fromShift(fromShift)
            .toShift(toShift)
            .handoverDate(LocalDate.now())
            .aiSummary(aiSummary)
            .createdBy(user)
            .build();

        handoverRepository.save(handover);
        log.info("인수인계 저장 완료");
    }

    @Transactional(readOnly = true)
    public List<Handover> getHandoversByDepartment(Long departmentId) {
        List<Handover> handovers = handoverRepository.findByDepartmentIdOrderByHandoverDateDesc(departmentId);
        // Lazy loading 강제 초기화
        handovers.forEach(h -> {
            h.getDepartment().getName();
            h.getFromShift().getType();
            h.getToShift().getType();
            h.getCreatedBy().getName();
        });
        return handovers;
    }

    @Transactional
    public void deleteHandover(Long handoverId, Long userId) {
        Handover handover = handoverRepository.findById(handoverId)
            .orElseThrow(() -> new IllegalArgumentException("인수인계를 찾을 수 없습니다."));
        
        // 작성자 본인만 삭제 가능
        if (!handover.getCreatedBy().getId().equals(userId)) {
            throw new IllegalArgumentException("본인이 작성한 인수인계만 삭제할 수 있습니다.");
        }
        
        handoverRepository.delete(handover);
        log.info("인수인계 삭제 완료 - handoverId: {}, userId: {}", handoverId, userId);
    }
}
