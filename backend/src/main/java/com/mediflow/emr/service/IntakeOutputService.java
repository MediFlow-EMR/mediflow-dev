package com.mediflow.emr.service;

import com.mediflow.emr.dto.IntakeOutputRequest;
import com.mediflow.emr.dto.IntakeOutputResponse;
import com.mediflow.emr.entity.IntakeOutput;
import com.mediflow.emr.entity.Patient;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.IntakeOutputRepository;
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
 * 섭취배설량 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntakeOutputService {

    private final IntakeOutputRepository intakeOutputRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    /**
     * 섭취배설량 등록
     */
    @Transactional
    public IntakeOutputResponse createIntakeOutput(Long nurseId, IntakeOutputRequest request) {
        log.info("섭취배설량 등록 - nurseId: {}, patientId: {}", nurseId, request.patientId());

        // 간호사 조회
        User nurse = userRepository.findById(nurseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 환자 조회
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PATIENT_NOT_FOUND));

        // 기록 시간 (요청에 없으면 현재 시간)
        LocalDateTime recordedAt = request.recordedAt() != null 
                ? request.recordedAt() 
                : LocalDateTime.now();

        // 미래 시간 검증
        if (recordedAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "기록 시간은 현재 시간보다 미래일 수 없습니다");
        }

        // 섭취배설량 생성
        IntakeOutput intakeOutput = IntakeOutput.builder()
                .patient(patient)
                .nurse(nurse)
                .intakeOral(request.intakeOral())
                .intakeIv(request.intakeIv())
                .outputUrine(request.outputUrine())
                .outputDrain(request.outputDrain())
                .recordedAt(recordedAt)
                .build();

        IntakeOutput saved = intakeOutputRepository.save(intakeOutput);
        log.info("섭취배설량 등록 완료 - id: {}", saved.getId());

        return toResponse(saved, nurseId);
    }

    /**
     * 환자의 섭취배설량 목록 조회 (최신순)
     */
    public List<IntakeOutputResponse> getPatientIntakeOutputs(Long patientId, Long currentUserId) {
        log.info("환자 섭취배설량 목록 조회 - patientId: {}, currentUserId: {}", patientId, currentUserId);

        // 환자 존재 확인
        if (!patientRepository.existsById(patientId)) {
            throw new BusinessException(ErrorCode.PATIENT_NOT_FOUND);
        }

        List<IntakeOutput> records = intakeOutputRepository.findByPatientIdOrderByRecordedAtDesc(patientId);
        log.info("조회된 I/O 기록 수: {}", records.size());

        return records.stream()
                .map(record -> toResponse(record, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 섭취배설량 수정
     */
    @Transactional
    public IntakeOutputResponse updateIntakeOutput(Long recordId, Long nurseId, IntakeOutputRequest request) {
        log.info("섭취배설량 수정 - recordId: {}, nurseId: {}", recordId, nurseId);

        // 기록 조회
        IntakeOutput record = intakeOutputRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IO_NOT_FOUND));

        // 권한 확인 (본인이 등록한 기록만 수정 가능)
        if (!record.getNurse().getId().equals(nurseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 등록한 기록만 수정할 수 있습니다");
        }

        // 기록 시간 (요청에 없으면 기존 시간 유지)
        LocalDateTime recordedAt = request.recordedAt() != null 
                ? request.recordedAt() 
                : record.getRecordedAt();

        // 미래 시간 검증
        if (recordedAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "기록 시간은 현재 시간보다 미래일 수 없습니다");
        }

        // 기록 수정
        record.update(
                request.intakeOral(),
                request.intakeIv(),
                request.outputUrine(),
                request.outputDrain(),
                recordedAt
        );

        log.info("섭취배설량 수정 완료 - id: {}", record.getId());
        return toResponse(record, nurseId);
    }

    /**
     * 섭취배설량 삭제
     */
    @Transactional
    public void deleteIntakeOutput(Long recordId, Long nurseId) {
        log.info("섭취배설량 삭제 - recordId: {}, nurseId: {}", recordId, nurseId);

        // 기록 조회
        IntakeOutput record = intakeOutputRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IO_NOT_FOUND));

        // 권한 확인 (본인이 등록한 기록만 삭제 가능)
        if (!record.getNurse().getId().equals(nurseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 등록한 기록만 삭제할 수 있습니다");
        }

        intakeOutputRepository.delete(record);
        log.info("섭취배설량 삭제 완료 - id: {}", recordId);
    }

    /**
     * Entity -> Response DTO 변환
     */
    private IntakeOutputResponse toResponse(IntakeOutput record, Long currentUserId) {
        boolean canEdit = currentUserId != null && 
                         record.getNurse().getId().equals(currentUserId);
        
        return IntakeOutputResponse.builder()
                .id(record.getId())
                .patientId(record.getPatient().getId())
                .patientName(record.getPatient().getName())
                .nurseId(record.getNurse().getId())
                .nurseName(record.getNurse().getName())
                .intakeOral(record.getIntakeOral())
                .intakeIv(record.getIntakeIv())
                .intakeTotal(record.getIntakeTotal())
                .outputUrine(record.getOutputUrine())
                .outputDrain(record.getOutputDrain())
                .outputTotal(record.getOutputTotal())
                .recordedAt(record.getRecordedAt())
                .createdAt(record.getCreatedAt())
                .canEdit(canEdit)
                .build();
    }
}
