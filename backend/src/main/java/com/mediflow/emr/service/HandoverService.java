package com.mediflow.emr.service;

import com.mediflow.emr.entity.*;
import com.mediflow.emr.dto.HandoverSaveRequest;
import com.mediflow.emr.dto.HandoverDto;
import com.mediflow.emr.entity.enums.Gender;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.config.GeminiConfig;
import com.mediflow.emr.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final HandoverRevisionRepository revisionRepository;
    private final AssignmentRepository assignmentRepository;
    private final GeminiConfig geminiConfig;
    private final ClinicalAuditService clinicalAuditService;

    @Transactional(readOnly = true)
    public String generateAiSummary(Long nurseId, Long fromShiftId) {
        log.info("AI 인수인계 요약 생성 - nurseId: {}, fromShiftId: {}", nurseId, fromShiftId);

        Shift fromShift = shiftRepository.findById(fromShiftId)
            .orElseThrow(() -> new IllegalArgumentException("근무조를 찾을 수 없습니다."));

        LocalDate shiftDate = fromShift.getDate();

        // 해당 근무조에 배정된 환자 조회
        List<Assignment> assignments = assignmentRepository.findByNurseIdAndShiftId(nurseId, fromShiftId);
        log.info("배정 수: {}, 조회 조건 - nurseId: {}, shiftId: {}", assignments.size(), nurseId, fromShiftId);
        
        if (assignments.isEmpty()) {
            log.warn("근무조({})에 배정된 환자가 없습니다 - nurseId: {}", fromShiftId, nurseId);
            return "현재 근무조에 배정된 환자가 없습니다.";
        }

        List<Patient> patients = assignments.stream()
            .map(Assignment::getPatient)
            .distinct()
            .collect(Collectors.toList());

        log.info("배정된 환자 수: {}", patients.size());
        // 환자 식별정보는 로그에 남기지 않는다.

        // 부서 정보는 첫 번째 환자의 부서로 설정
        DepartmentEntity department = patients.get(0).getDepartment();

        final LocalDateTime shiftStart = LocalDateTime.of(shiftDate, fromShift.getStartTime());
        LocalDateTime tempEnd = LocalDateTime.of(shiftDate, fromShift.getEndTime());
        
        if (fromShift.getEndTime().isBefore(fromShift.getStartTime())) {
            tempEnd = tempEnd.plusDays(1);
        }
        final LocalDateTime shiftEnd = tempEnd;

        List<PatientData> patientDataList = patients.stream()
            .map(patient -> collectPatientData(patient, shiftStart, shiftEnd))
            .sorted((a, b) -> Boolean.compare(b.important, a.important))
            .collect(Collectors.toList());

        log.info("수집된 환자 데이터 수: {}", patientDataList.size());

        List<PatientData> recordedPatients = patientDataList.stream().filter(PatientData::hasData).toList();
        if (recordedPatients.isEmpty()) {
            return patientDataList.stream().map(this::missingRecordSummary).collect(Collectors.joining("\n\n"));
        }
        String prompt = buildPrompt(department, fromShift, recordedPatients);
        log.info("생성된 프롬프트 길이: {}", prompt.length());
        String deidentifiedSummary = geminiService.generateContent(prompt);
        String recordedSummary = restoreLocalPatientLabels(deidentifiedSummary, recordedPatients);
        String missingSummary = patientDataList.stream().filter(data -> !data.hasData())
                .map(this::missingRecordSummary).collect(Collectors.joining("\n\n"));
        return missingSummary.isBlank() ? recordedSummary : recordedSummary + "\n\n" + missingSummary;
    }

    @Transactional(readOnly = true)
    public String generatePatientAiSummary(Long nurseId, Long fromShiftId, Long patientId) {
        Shift fromShift = shiftRepository.findById(fromShiftId)
            .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "근무조를 찾을 수 없습니다."));

        Patient patient = assignmentRepository.findByNurseIdAndShiftId(nurseId, fromShiftId)
            .stream()
            .map(Assignment::getPatient)
            .filter(assignedPatient -> assignedPatient.getId().equals(patientId))
            .findFirst()
            .orElseThrow(() -> new BusinessException(
                ErrorCode.FORBIDDEN,
                "현재 근무조에 배정된 환자만 다시 생성할 수 있습니다."
            ));

        LocalDate shiftDate = fromShift.getDate();
        LocalDateTime shiftStart = LocalDateTime.of(shiftDate, fromShift.getStartTime());
        LocalDateTime shiftEnd = LocalDateTime.of(shiftDate, fromShift.getEndTime());
        if (fromShift.getEndTime().isBefore(fromShift.getStartTime())) {
            shiftEnd = shiftEnd.plusDays(1);
        }

        PatientData patientData = collectPatientData(patient, shiftStart, shiftEnd);
        if (!patientData.hasData()) return missingRecordSummary(patientData);
        List<PatientData> patientDataList = List.of(patientData);
        String prompt = buildPrompt(patient.getDepartment(), fromShift, patientDataList);
        String deidentifiedSummary = geminiService.generateContent(prompt);

        log.info("환자 단위 AI 인수인계 재생성 완료 - nurseId: {}, patientId: {}", nurseId, patientId);
        return restoreLocalPatientLabels(deidentifiedSummary, patientDataList);
    }

    /**
     * 외부 AI에는 가명 식별자만 전송하고, 응답이 병원 서버로 돌아온 뒤에만
     * 실제 환자 표시명과 차트번호를 다시 결합한다.
     */
    private String restoreLocalPatientLabels(String summary, List<PatientData> dataList) {
        String restored = summary;
        for (int index = 0; index < dataList.size(); index++) {
            Patient patient = dataList.get(index).patient;
            String safeLabel = String.format("환자-%03d", index + 1);
            String localLabel = patient.getName() + " (" + patient.getChartNumber() + ")";
            restored = restored.replace(safeLabel, localLabel);
        }

        // AI가 반환한 식별자를 검증하며 응답 순서로 환자를 추측하지 않는다.
        Pattern patientHeading = Pattern.compile("(?m)^(?:#{1,6}[\\t ]*)?(?:\\*\\*)?\\[([^\\n\\]]+)\\](?:\\*\\*)?[\\t ]*$");
        Matcher matcher = patientHeading.matcher(restored);
        StringBuffer normalized = new StringBuffer();
        Set<Long> matchedPatients = new HashSet<>();
        while (matcher.find()) {
            String headingText = matcher.group(1);
            Patient patient = dataList.stream().map(data -> data.patient)
                    .filter(candidate -> headingText.contains("(" + candidate.getChartNumber() + ")"))
                    .findFirst().orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST,
                            "AI 응답의 환자 식별자가 일치하지 않습니다. 초안을 다시 생성해 주세요."));
            if (!matchedPatients.add(patient.getId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "AI 응답에 중복된 환자가 있습니다. 다시 생성해 주세요.");
            }
            String gender = patient.getGender() == Gender.M ? "남" : "여";
            String heading = String.format(
                "**[%s (%s, %d세/%s)]**",
                patient.getName(), patient.getChartNumber(), patient.getAge(), gender
            );
            matcher.appendReplacement(normalized, Matcher.quoteReplacement(heading));
        }
        matcher.appendTail(normalized);
        if (matchedPatients.size() != dataList.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI 응답에 환자 구분이 누락되었습니다. 다시 생성해 주세요.");
        }
        return normalized.toString();
    }

    private String missingRecordSummary(PatientData data) {
        Patient patient = data.patient;
        return String.format("**[%s (%s, %d세/%s)]**\n- 주요 변화: 해당 근무조 기록 없음.\n"
                        + "- 수행한 처치: 해당 근무조 기록 없음.\n- 지속 관찰 사항: 확인 가능한 기록이 없어 판단할 수 없음.",
                patient.getName(), patient.getChartNumber(), patient.getAge(), patient.getGender() == Gender.M ? "남" : "여");
    }

    private PatientData collectPatientData(Patient patient, LocalDateTime start, LocalDateTime end) {
        PatientData data = new PatientData();
        data.patient = patient;

        data.notes = nursingNoteRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
            .stream()
            .filter(n -> n.getCreatedAt() != null && !n.getCreatedAt().isBefore(start) && n.getCreatedAt().isBefore(end))
            .collect(Collectors.toList());

        data.important = data.notes.stream().anyMatch(NursingNote::getIsImportant);

        data.vitals = vitalSignRepository.findByPatientIdAndMeasuredAtBetween(patient.getId(), start, end.minusNanos(1));
        data.important = data.important || data.vitals.stream().anyMatch(this::isAbnormal);

        data.medications = medicationRepository.findByPatientIdAndAdministeredAtBetween(patient.getId(), start, end.minusNanos(1));
        data.intakeOutputs = intakeOutputRepository.findByPatientIdAndRecordedAtBetween(patient.getId(), start, end.minusNanos(1));
        data.important = data.important || data.intakeOutputs.stream().anyMatch(this::hasImbalance);

        data.testResults = testResultRepository.findByPatientIdOrderByResultDateDesc(patient.getId())
            .stream()
            .filter(r -> r.getResultDate() != null && !r.getResultDate().isBefore(start) && r.getResultDate().isBefore(end))
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

        int patientSequence = 1;
        for (PatientData data : dataList) {
            Patient p = data.patient;
            // 외부 AI에는 이름과 차트번호를 보내지 않고 요청 내 가명 식별자를 사용한다.
            String safePatientLabel = String.format("환자-%03d", patientSequence++);
            sb.append("[").append(safePatientLabel)
              .append(" (")
              .append(p.getAge()).append("세/")
              .append(p.getGender() == Gender.M ? "남" : "여")
              .append(")]\n");

            if (!data.notes.isEmpty()) {
                sb.append("- 간호기록:\n");
                data.notes.forEach(n -> sb.append("  * ").append(n.getCreatedAt().toLocalTime())
                    .append(" ").append(n.getPlainText()).append("\n"));
            }

            if (!data.vitals.isEmpty()) {
                VitalSign v = data.vitals.stream()
                        .max(Comparator.comparing(VitalSign::getMeasuredAt,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                        .orElseThrow();
                sb.append("- 바이탈: ");
                if (v.getSystolicBp() != null) sb.append("BP ").append(v.getSystolicBp()).append("/").append(v.getDiastolicBp()).append(", ");
                if (v.getHeartRate() != null) sb.append("HR ").append(v.getHeartRate()).append(", ");
                if (v.getBodyTemp() != null) sb.append("Temp ").append(v.getBodyTemp()).append(", ");
                if (v.getSpo2() != null) sb.append("SpO2 ").append(v.getSpo2()).append("%");
                sb.append("\n");
            }

            if (!data.testResults.isEmpty()) {
                sb.append("- 검사결과: ");
                data.testResults.forEach(t -> {
                    sb.append(t.getTestName());
                    if (t.getResultValue() != null) sb.append(": ").append(t.getResultValue());
                    if (t.getReferenceRange() != null) sb.append(" (참고치 ").append(t.getReferenceRange()).append(")");
                    if (Boolean.TRUE.equals(t.getIsAbnormal())) sb.append(" [이상]");
                    sb.append(", ");
                });
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
                long intake = data.intakeOutputs.stream().mapToLong(IntakeOutput::getIntakeTotal).sum();
                long output = data.intakeOutputs.stream().mapToLong(IntakeOutput::getOutputTotal).sum();
                long balance = intake - output;
                String direction = balance > 0 ? "섭취 초과" : balance < 0 ? "배설 초과" : "섭취·배설량 동일";
                sb.append("- I/O: 섭취 ").append(intake).append("mL, 배설 ").append(output)
                        .append("mL, 차이(섭취-배설) ").append(balance > 0 ? "+" : "").append(balance)
                        .append("mL (").append(direction).append(")\n");
            }

            sb.append("\n");
        }

        sb.append("\n각 환자별로 다음 형식으로 인수인계문을 작성해줘:\n\n");
        sb.append("**[환자-001 (나이/성별)]**\n");
        sb.append("- 주요 변화: 특이사항 및 상태 변화\n");
        sb.append("- 수행한 처치: 투약, 검사 등\n");
        sb.append("- 지속 관찰 사항: 다음 근무조에서 주의할 점\n\n");
        sb.append("환자당 3-5문장, 중요한 환자는 더 자세히 작성. 간결하고 명확하게.\n");
        sb.append("제공된 기록에 있는 사실만 요약하고 기록에 없는 수치, 약물, 처치, 환자명, 상태를 만들지 마세요.\n");
        sb.append("누락된 정보는 '기록 없음'으로 표시하고 '정상', '안정적', '처치 완료'로 추정하지 마세요.\n");
        sb.append("수분 차이는 제공한 수치와 섭취 초과·배설 초과·동일로만 표현하세요. 긍정적인·부정적인 수분 균형이나 상태가 좋다는 의미로 해석하지 마세요.\n");
        sb.append("입력의 환자-001 등 가명 식별자를 그대로 사용하며 환자마다 반드시 **[식별자 (나이/성별)]** 제목을 작성하세요.");

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

    private void requireDepartment(Long departmentId, User user) {
        if (user.getDepartment() == null || !Objects.equals(user.getDepartment().getId(), departmentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "소속 부서의 인수인계만 접근할 수 있습니다.");
        }
    }

    private Handover accessibleHandover(Long id, User user, boolean editing) {
        Handover record = handoverRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "인수인계를 찾을 수 없습니다."));
        requireDepartment(record.getDepartment().getId(), user);
        if ((editing || "DRAFT".equals(record.getStatus())) && !Objects.equals(record.getCreatedBy().getId(), user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "작성자만 초안을 조회하거나 인수인계를 수정할 수 있습니다.");
        }
        return record;
    }

    private void validateContent(HandoverSaveRequest request) {
        String source = request.sourceType() == null ? "AI" : request.sourceType();
        String status = request.status() == null ? "APPROVED" : request.status();
        if (!Set.of("AI", "MANUAL").contains(source) || !Set.of("DRAFT", "APPROVED").contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "올바른 작성 방식과 저장 상태를 선택해주세요.");
        }
        if (request.finalSummary() == null || request.finalSummary().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "인수인계 내용을 입력해주세요.");
        }
        if ("AI".equals(source) && (request.aiDraft() == null || request.aiDraft().isBlank())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI 원본 초안이 필요합니다.");
        }
        if (Objects.equals(request.fromShiftId(), request.toShiftId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "인계 근무조와 인수 근무조는 달라야 합니다.");
        }
    }

    private void snapshot(Handover record, User editor) {
        revisionRepository.save(HandoverRevision.builder()
                .handoverId(record.getId()).editedById(editor.getId())
                .editedByName(editor.getName() == null ? editor.getNickname() : editor.getName())
                .recordedAt(LocalDateTime.now()).recordVersion(record.getVersion())
                .status(record.getStatus()).sourceType(record.getSourceType())
                .aiSummary(record.getAiSummary()).finalSummary(record.getFinalSummary()).aiModel(record.getAiModel()).build());
    }

    @Transactional
    public HandoverDto saveHandover(HandoverSaveRequest request, User user) {
        requireDepartment(request.departmentId(), user);
        validateContent(request);
        DepartmentEntity department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));
        Shift from = shiftRepository.findById(request.fromShiftId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "인계 근무조를 찾을 수 없습니다."));
        Shift to = shiftRepository.findById(request.toShiftId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "인수 근무조를 찾을 수 없습니다."));
        Handover record = Handover.builder().department(department).fromShift(from).toShift(to)
                .handoverDate(from.getDate()).createdBy(user).build();
        applyContent(record, request);
        Handover saved = handoverRepository.saveAndFlush(record);
        snapshot(saved, user);
        clinicalAuditService.record(user.getId(), null, saved.getStatus().equals("DRAFT") ? "SAVE_DRAFT" : "APPROVE",
                "HANDOVER", saved.getId(), "source=" + saved.getSourceType() + "; version=" + saved.getVersion());
        return HandoverDto.from(saved);
    }

    private void applyContent(Handover record, HandoverSaveRequest request) {
        String source = request.sourceType() == null ? "AI" : request.sourceType();
        record.updateContent("AI".equals(source) ? request.aiDraft() : null, request.finalSummary(), source,
                request.status() == null ? "APPROVED" : request.status(),
                "AI".equals(source) ? (record.getAiModel() == null || !Objects.equals(record.getAiSummary(), request.aiDraft()) ? geminiConfig.getModel() : record.getAiModel()) : null);
    }

    @Transactional
    public HandoverDto updateHandover(Long id, HandoverSaveRequest request, User user) {
        Handover record = accessibleHandover(id, user, true);
        validateContent(request);
        if (request.version() == null || !Objects.equals(record.getVersion(), request.version())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        if (!Objects.equals(record.getDepartment().getId(), request.departmentId()) ||
                !Objects.equals(record.getFromShift().getId(), request.fromShiftId()) ||
                !Objects.equals(record.getToShift().getId(), request.toShiftId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "기존 인수인계의 부서와 근무조는 변경할 수 없습니다.");
        }
        if ("APPROVED".equals(record.getStatus()) &&
                (!"APPROVED".equals(request.status() == null ? "APPROVED" : request.status()) ||
                !Objects.equals(record.getSourceType(), request.sourceType() == null ? "AI" : request.sourceType()) ||
                !Objects.equals(record.getAiSummary(), request.aiDraft()))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "승인된 인수인계의 상태와 작성 원본은 변경할 수 없습니다.");
        }
        // Preserve a baseline for records created before revision tracking was added.
        if (!revisionRepository.existsByHandoverId(id)) snapshot(record, record.getCreatedBy());
        applyContent(record, request);
        Handover saved = handoverRepository.saveAndFlush(record);
        snapshot(saved, user);
        clinicalAuditService.record(user.getId(), null, "UPDATE", "HANDOVER", saved.getId(),
                "status=" + saved.getStatus() + "; version=" + saved.getVersion());
        return HandoverDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<HandoverDto> getHandoversByDepartment(Long departmentId, User user) {
        requireDepartment(departmentId, user);
        return handoverRepository.findByDepartmentIdOrderByHandoverDateDesc(departmentId).stream()
                .filter(record -> !"DRAFT".equals(record.getStatus()) || Objects.equals(record.getCreatedBy().getId(), user.getId()))
                .map(HandoverDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<HandoverRevision> getRevisions(Long id, User user) {
        accessibleHandover(id, user, false);
        return revisionRepository.findByHandoverIdOrderByIdDesc(id);
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
