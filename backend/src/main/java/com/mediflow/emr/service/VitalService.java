package com.mediflow.emr.service;

import com.mediflow.emr.util.RecordUpdateGuard;

import com.mediflow.emr.dto.VitalSignRequest;
import com.mediflow.emr.dto.VitalSignResponse;
import com.mediflow.emr.entity.Patient;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.entity.VitalSign;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.PatientRepository;
import com.mediflow.emr.repository.UserRepository;
import com.mediflow.emr.repository.VitalSignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 바이탈 사인 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VitalService {

    private final VitalSignRepository vitalSignRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PatientAccessService patientAccessService;

    /**
     * 바이탈 사인 등록
     */
    @Transactional
    public VitalSignResponse createVitalSign(Long nurseId, VitalSignRequest request) {
        log.info("바이탈 사인 등록 - nurseId: {}, patientId: {}", nurseId, request.patientId());

        // 간호사 조회
        User nurse = userRepository.findById(nurseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 환자 조회
        Patient patient = patientAccessService.requireAccessiblePatient(nurseId, request.patientId());

        // 측정 시간 (요청에 없으면 현재 시간)
        LocalDateTime measuredAt = request.measuredAt() != null 
                ? request.measuredAt() 
                : LocalDateTime.now();

        // 미래 시간 검증
        if (measuredAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "측정 시간은 현재 시간보다 미래일 수 없습니다");
        }

        // 바이탈 사인 생성
        VitalSign vitalSign = VitalSign.builder()
                .patient(patient)
                .nurse(nurse)
                .systolicBp(request.systolicBp())
                .diastolicBp(request.diastolicBp())
                .heartRate(request.heartRate())
                .bodyTemp(request.bodyTemp())
                .respiratoryRate(request.respiratoryRate())
                .spo2(request.spo2())
                .measuredAt(measuredAt)
                .build();

        VitalSign saved = vitalSignRepository.save(vitalSign);
        log.info("바이탈 사인 등록 완료 - id: {}", saved.getId());

        return toResponse(saved);
    }

    /**
     * 환자의 바이탈 사인 목록 조회 (최신순)
     */
    public List<VitalSignResponse> getPatientVitals(Long patientId, Long currentUserId) {
        log.info("환자 바이탈 목록 조회 - patientId: {}, currentUserId: {}", patientId, currentUserId);

        // 환자 존재 확인
        patientAccessService.requireAccessiblePatient(currentUserId, patientId);

        List<VitalSign> vitals = vitalSignRepository.findByPatientIdOrderByMeasuredAtDesc(patientId);
        log.info("조회된 바이탈 수: {}", vitals.size());

        return vitals.stream()
                .map(vital -> toResponse(vital, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 환자의 최근 바이탈 사인 조회
     */
    public VitalSignResponse getLatestVital(Long patientId, Long currentUserId) {
        log.info("최근 바이탈 조회 - patientId: {}", patientId);

        patientAccessService.requireAccessiblePatient(currentUserId, patientId);

        List<VitalSign> vitals = vitalSignRepository.findByPatientIdOrderByMeasuredAtDesc(patientId);
        
        if (vitals.isEmpty()) {
            return null;
        }

        return toResponse(vitals.get(0));
    }

    /**
     * 바이탈 사인 수정
     */
    @Transactional
    public VitalSignResponse updateVitalSign(Long vitalId, Long nurseId, VitalSignRequest request) {
        log.info("바이탈 사인 수정 - vitalId: {}, nurseId: {}", vitalId, nurseId);

        // 바이탈 사인 조회
        VitalSign vitalSign = vitalSignRepository.findById(vitalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VITAL_NOT_FOUND));

        patientAccessService.requireAccessiblePatient(nurseId, vitalSign.getPatient().getId());
        RecordUpdateGuard.requireCurrent(vitalSign.getPatient().getId(), request.patientId(), vitalSign.getVersion(), request.version());

        // 권한 확인 (본인이 등록한 바이탈만 수정 가능)
        if (!vitalSign.getNurse().getId().equals(nurseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 등록한 바이탈만 수정할 수 있습니다");
        }

        // 측정 시간 (요청에 없으면 기존 시간 유지)
        LocalDateTime measuredAt = request.measuredAt() != null 
                ? request.measuredAt() 
                : vitalSign.getMeasuredAt();

        // 미래 시간 검증
        if (measuredAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "측정 시간은 현재 시간보다 미래일 수 없습니다");
        }

        // 바이탈 사인 수정
        vitalSign.update(
                request.systolicBp(),
                request.diastolicBp(),
                request.heartRate(),
                request.bodyTemp(),
                request.respiratoryRate(),
                request.spo2(),
                measuredAt
        );

        log.info("바이탈 사인 수정 완료 - id: {}", vitalSign.getId());
        vitalSignRepository.saveAndFlush(vitalSign);
        return toResponse(vitalSign, nurseId);
    }

    /**
     * 바이탈 사인 삭제
     * 원본 보존 정책에 따라 직접 삭제하지 않는다.
     */
    @Transactional
    public void deleteVitalSign(Long vitalId, Long nurseId) {
        log.info("바이탈 사인 삭제 시도 - vitalId: {}, nurseId: {}", vitalId, nurseId);

        // 바이탈 사인 조회
        VitalSign vitalSign = vitalSignRepository.findById(vitalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VITAL_NOT_FOUND));

        // 권한 확인 (본인이 등록한 바이탈만 삭제 가능)
        if (!vitalSign.getNurse().getId().equals(nurseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 등록한 바이탈만 삭제할 수 있습니다");
        }

        // 원본 보존 정책.
        // 삭제 대신 예외를 발생시켜 삭제 불가 안내
        log.warn("원본 보존 정책에 따라 바이탈 사인 삭제 불가 - vitalId: {}", vitalId);
        throw new BusinessException(ErrorCode.MEDICAL_RECORD_CANNOT_BE_DELETED);

    }

    /**
     * Entity -> Response DTO 변환
     */
    private VitalSignResponse toResponse(VitalSign vitalSign) {
        return toResponse(vitalSign, null);
    }

    /**
     * Entity -> Response DTO 변환 (수정 가능 여부 포함)
     */
    private VitalSignResponse toResponse(VitalSign vitalSign, Long currentUserId) {
        boolean canEdit = currentUserId != null && 
                         vitalSign.getNurse().getId().equals(currentUserId);
        
        return VitalSignResponse.builder()
                .id(vitalSign.getId())
                .version(vitalSign.getVersion())
                .patientId(vitalSign.getPatient().getId())
                .patientName(vitalSign.getPatient().getName())
                .nurseId(vitalSign.getNurse().getId())
                .nurseName(vitalSign.getNurse().getName())
                .systolicBp(vitalSign.getSystolicBp())
                .diastolicBp(vitalSign.getDiastolicBp())
                .heartRate(vitalSign.getHeartRate())
                .bodyTemp(vitalSign.getBodyTemp())
                .respiratoryRate(vitalSign.getRespiratoryRate())
                .spo2(vitalSign.getSpo2())
                .measuredAt(vitalSign.getMeasuredAt())
                .createdAt(vitalSign.getCreatedAt())
                .canEdit(canEdit)
                .build();
    }
}
