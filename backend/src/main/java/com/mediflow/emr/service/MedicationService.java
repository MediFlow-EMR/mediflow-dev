package com.mediflow.emr.service;

import com.mediflow.emr.dto.MedicationRequest;
import com.mediflow.emr.dto.MedicationResponse;
import com.mediflow.emr.entity.Medication;
import com.mediflow.emr.entity.MedicalOrder;
import com.mediflow.emr.entity.Patient;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.MedicationRepository;
import com.mediflow.emr.repository.MedicalOrderRepository;
import com.mediflow.emr.repository.PatientRepository;
import com.mediflow.emr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 투약 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PatientAccessService patientAccessService;
    private final MedicalOrderRepository medicalOrderRepository;
    private final ClinicalAuditService clinicalAuditService;

    /**
     * 투약 기록 등록
     */
    @Transactional
    public MedicationResponse createMedication(Long nurseId, MedicationRequest request) {
        log.info("투약 기록 등록 - nurseId: {}, patientId: {}", nurseId, request.patientId());

        // 간호사 조회
        User nurse = userRepository.findById(nurseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 환자 조회
        Patient patient = patientAccessService.requireAccessiblePatient(nurseId, request.patientId());

        MedicalOrder order = medicalOrderRepository.findById(request.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "선택한 처방과 환자가 일치하지 않습니다");
        }
        if (!"MEDICATION".equals(order.getOrderType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "투약 처방만 투약 수행으로 기록할 수 있습니다");
        }
        if ("COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "이미 완료되었거나 취소된 처방입니다");
        }

        // 투약 시간 (요청에 없으면 현재 시간)
        LocalDateTime administeredAt = request.administeredAt() != null 
                ? request.administeredAt() 
                : LocalDateTime.now();

        // 미래 시간 검증
        if (administeredAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "투약 시간은 현재 시간보다 미래일 수 없습니다");
        }

        // 투약 기록 생성
        Medication medication = Medication.builder()
                .patient(patient)
                .nurse(nurse)
                .medicalOrder(order)
                .drugName(order.getOrderName())
                .drugCode(order.getOrderCode())
                .dose(order.getDose())
                .route(parseRoute(order.getRoute()))
                .frequency(order.getFrequency())
                .administeredAt(administeredAt)
                .orderDoctor(order.getOrderDoctor())
                .administrationNotes(request.notes())
                .build();

        Medication saved = medicationRepository.save(medication);
        order.updateStatus("COMPLETED", nurse.getName());
        medicalOrderRepository.save(order);
        clinicalAuditService.record(nurseId, patient.getId(), "ADMINISTER",
                "MEDICATION", saved.getId(), "orderId=" + order.getId());
        log.info("투약 기록 등록 완료 - id: {}", saved.getId());

        return toResponse(saved, nurseId);
    }

    /**
     * 환자의 투약 기록 목록 조회 (최신순)
     */
    public List<MedicationResponse> getPatientMedications(Long patientId, Long currentUserId) {
        log.info("환자 투약 목록 조회 - patientId: {}, currentUserId: {}", patientId, currentUserId);

        // 환자 존재 확인
        patientAccessService.requireAccessiblePatient(currentUserId, patientId);

        List<Medication> medications = medicationRepository.findByPatientIdOrderByAdministeredAtDesc(patientId);
        log.info("조회된 투약 기록 수: {}", medications.size());

        return medications.stream()
                .map(med -> toResponse(med, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 투약 기록 수정
     * 의료법 제23조에 따라 의료 기록은 10년간 보존해야 하므로 수정 불가
     */
    @Transactional
    public MedicationResponse updateMedication(Long medicationId, Long nurseId, MedicationRequest request) {
        log.info("투약 기록 수정 시도 - medicationId: {}, nurseId: {}", medicationId, nurseId);

        // 기록 조회
        Medication medication = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDICATION_NOT_FOUND));

        // 권한 확인 (본인이 등록한 기록만 수정 가능)
        if (!medication.getNurse().getId().equals(nurseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 등록한 기록만 수정할 수 있습니다");
        }

        // 의료법 제23조: 의료 기록은 10년간 보존 의무
        // 투약 기록은 수정 불가
        log.warn("의료법 제23조에 따라 투약 기록 수정 불가 - medicationId: {}", medicationId);
        throw new BusinessException(ErrorCode.MEDICAL_RECORD_CANNOT_BE_DELETED);

        // 기존 수정 코드 (주석 처리)
        /*
        // 투약 시간 (요청에 없으면 기존 시간 유지)
        LocalDateTime administeredAt = request.administeredAt() != null 
                ? request.administeredAt() 
                : medication.getAdministeredAt();

        // 미래 시간 검증
        if (administeredAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "투약 시간은 현재 시간보다 미래일 수 없습니다");
        }

        // 기록 수정
        medication.update(
                request.drugName(),
                request.drugCode(),
                request.dose(),
                request.route(),
                request.frequency(),
                administeredAt,
                request.orderDoctor()
        );

        log.info("투약 기록 수정 완료 - id: {}", medication.getId());
        return toResponse(medication, nurseId);
        */
    }

    /**
     * 투약 기록 삭제
     * 의료법 제23조에 따라 의료 기록은 10년간 보존해야 하므로 삭제 불가
     */
    @Transactional
    public void deleteMedication(Long medicationId, Long nurseId) {
        log.info("투약 기록 삭제 시도 - medicationId: {}, nurseId: {}", medicationId, nurseId);

        // 기록 조회
        Medication medication = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDICATION_NOT_FOUND));

        // 권한 확인 (본인이 등록한 기록만 삭제 가능)
        if (!medication.getNurse().getId().equals(nurseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 등록한 기록만 삭제할 수 있습니다");
        }

        // 의료법 제23조: 의료 기록은 10년간 보존 의무
        // 삭제 대신 예외를 발생시켜 삭제 불가 안내
        log.warn("의료법 제23조에 따라 투약 기록 삭제 불가 - medicationId: {}", medicationId);
        throw new BusinessException(ErrorCode.MEDICAL_RECORD_CANNOT_BE_DELETED);

        // 기존 삭제 코드 (주석 처리)
        // medicationRepository.delete(medication);
        // log.info("투약 기록 삭제 완료 - id: {}", medicationId);
    }

    /**
     * Entity -> Response DTO 변환
     */
    private MedicationResponse toResponse(Medication medication, Long currentUserId) {
        boolean canEdit = currentUserId != null && 
                         medication.getNurse().getId().equals(currentUserId);
        
        return MedicationResponse.builder()
                .id(medication.getId())
                .patientId(medication.getPatient().getId())
                .orderId(medication.getMedicalOrder() == null ? null : medication.getMedicalOrder().getId())
                .patientName(medication.getPatient().getName())
                .nurseId(medication.getNurse().getId())
                .nurseName(medication.getNurse().getName())
                .drugName(medication.getDrugName())
                .drugCode(medication.getDrugCode())
                .dose(medication.getDose())
                .route(medication.getRoute())
                .frequency(medication.getFrequency())
                .administeredAt(medication.getAdministeredAt())
                .orderDoctor(medication.getOrderDoctor())
                .notes(medication.getAdministrationNotes())
                .createdAt(medication.getCreatedAt())
                .canEdit(canEdit)
                .build();
    }

    private com.mediflow.emr.entity.enums.MedicationRoute parseRoute(String route) {
        try {
            return com.mediflow.emr.entity.enums.MedicationRoute.valueOf(route);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "처방의 투약 경로가 올바르지 않습니다");
        }
    }
}
