package com.mediflow.emr.service;

import com.mediflow.emr.entity.Patient;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.entity.enums.Role;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.PatientRepository;
import com.mediflow.emr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 환자 임상정보 접근의 공통 경계.
 * 관리자 외 사용자는 활성 계정이어야 하며 자신의 소속 부서 환자만 접근한다.
 * 담당 환자만으로 더 좁히는 정책은 병원 정책에 따라 이 경계에서 확장한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientAccessService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final ClinicalAuditService clinicalAuditService;

    public Patient requireAccessiblePatient(Long userId, Long patientId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATIENT_NOT_FOUND));

        assertActive(user);
        if (user.getRole() == Role.ADMIN) {
            clinicalAuditService.record(userId, patientId, "READ", "PATIENT", patientId, "관리자 환자 접근");
            return patient;
        }

        boolean sameDepartment = user.getDepartment() != null
                && patient.getDepartment() != null
                && user.getDepartment().getId().equals(patient.getDepartment().getId());
        boolean clinicalRole = user.getRole() == Role.NURSE || user.getRole() == Role.DOCTOR;

        if (!clinicalRole || !sameDepartment) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "소속 부서 환자만 조회하거나 기록할 수 있습니다.");
        }
        clinicalAuditService.record(userId, patientId, "READ", "PATIENT", patientId, "소속 부서 환자 접근");
        return patient;
    }

    public Long requireUserId(String providerId) {
        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        assertActive(user);
        return user.getId();
    }

    private void assertActive(User user) {
        if (!user.getIsActive() || user.getIsLocked()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "사용할 수 없는 계정입니다.");
        }
    }
}
