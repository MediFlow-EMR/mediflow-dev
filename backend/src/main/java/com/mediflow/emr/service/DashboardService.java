package com.mediflow.emr.service;

import com.mediflow.emr.dto.DepartmentSummaryResponse;
import com.mediflow.emr.dto.MyPatientResponse;
import com.mediflow.emr.entity.*;
import com.mediflow.emr.entity.enums.ShiftType;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 대시보드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final VitalSignRepository vitalSignRepository;
    private final PatientRepository patientRepository;
    private final ShiftRepository shiftRepository;

    /**
     * 내 담당 환자 목록 조회
     */
    public List<MyPatientResponse> getMyPatients(Long userId) {
        return getMyPatients(userId, null, null);
    }

    public List<MyPatientResponse> getMyPatients(Long userId, LocalDate date, ShiftType shiftType) {
        // 사용자 조회
        User nurse = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 오늘 날짜
        LocalDate today = date == null ? LocalDate.now() : date;

        // 현재 시간 기준 근무조 확인
        ShiftType currentShiftType = shiftType == null ? getCurrentShiftType() : shiftType;

        // 오늘 날짜의 현재 근무조 조회
        Shift currentShift = shiftRepository.findByDateAndType(today, currentShiftType)
                .orElse(null);

        if (currentShift == null) {
            log.warn("오늘 날짜의 근무조를 찾을 수 없습니다. userId: {}, date: {}, shiftType: {}", 
                    userId, today, currentShiftType);
            return List.of();
        }

        // 배정된 환자 목록 조회
        List<Assignment> assignments = assignmentRepository.findByNurseIdAndShiftId(userId, currentShift.getId());

        // 환자 정보 + 최근 바이탈 조합
        List<MyPatientResponse> responses = assignments.stream()
                .map(assignment -> {
                    Patient patient = assignment.getPatient();
                    
                    // 최근 바이탈 조회
                    List<VitalSign> vitals = vitalSignRepository.findByPatientIdOrderByMeasuredAtDesc(patient.getId());
                    VitalSign latestVital = vitals.isEmpty() ? null : vitals.get(0);

                    return MyPatientResponse.builder()
                            .patientId(patient.getId())
                            .chartNumber(patient.getChartNumber())
                            .name(patient.getName())
                            .age(patient.getAge())
                            .gender(patient.getGender())
                            .diagnosis(patient.getDiagnosis())
                            .allergies(patient.getAllergies())
                            .triageLevel(patient.getTriageLevel())
                            .isPrimary(false) // 주담당 기능 제거
                            .systolicBp(latestVital != null ? latestVital.getSystolicBp() : null)
                            .diastolicBp(latestVital != null ? latestVital.getDiastolicBp() : null)
                            .heartRate(latestVital != null ? latestVital.getHeartRate() : null)
                            .bodyTemp(latestVital != null ? latestVital.getBodyTemp() : null)
                            .respiratoryRate(latestVital != null ? latestVital.getRespiratoryRate() : null)
                            .spo2(latestVital != null ? latestVital.getSpo2() : null)
                            .lastVitalTime(latestVital != null ? latestVital.getMeasuredAt() : null)
                            .departmentName(patient.getDepartment() != null ? patient.getDepartment().getName() : null)
                            .departmentCode(patient.getDepartment() != null ? patient.getDepartment().getCode() : null)
                            .admissionDate(patient.getAdmissionDate())
                            .roomNumber(patient.getRoomNumber())
                            .bedNumber(patient.getBedNumber())
                            .attendingPhysician(patient.getAttendingPhysician())
                            .fallRisk(patient.getFallRisk())
                            .isolationType(patient.getIsolationType())
                            .build();
                })
                .collect(Collectors.toList());

        // 응급실 환자는 트리아지 순서로 정렬 (낮은 숫자가 우선)
        responses.sort((a, b) -> {
            // 트리아지가 있는 환자 우선
            if (a.getTriageLevel() != null && b.getTriageLevel() == null) return -1;
            if (a.getTriageLevel() == null && b.getTriageLevel() != null) return 1;
            if (a.getTriageLevel() != null && b.getTriageLevel() != null) {
                return a.getTriageLevel().compareTo(b.getTriageLevel());
            }
            return 0;
        });

        return responses;
    }

    /** 전체 환자 목록과 현재 근무조 배정을 조회한다. */
    public List<MyPatientResponse> getAllPatients(Long userId) {
        // 1단계: 사용자 조회 및 유효성 검증
        User nurse = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (nurse.getDepartment() == null) {
            log.warn("간호사의 부서가 설정되지 않았습니다. userId: {}", userId);
            return List.of();
        }

        // 2단계: 부서의 입원 중인 모든 환자 조회
        // 쿼리 1회: SELECT * FROM patient WHERE department_id = ? AND is_admitted = true
        List<Patient> patients = patientRepository.findByDepartmentIdAndIsAdmitted(
                nurse.getDepartment().getId(), true);

        if (patients.isEmpty()) {
            return List.of();
        }
        // 성능 최적화: 반복되는 DB 조회를 미리 한 번에 처리

        // 3단계: 현재 근무조 정보를 미리 조회 (한 번만)
        // [최적화 전] 각 환자마다 조회 → 환자 10명이면 10번 쿼리
        // [최적화 후] 반복문 밖에서 한 번만 조회 → 1번 쿼리
        LocalDate today = LocalDate.now();
        ShiftType currentShiftType = getCurrentShiftType();

        // 쿼리 1회: SELECT * FROM shift WHERE date = ? AND type = ?
        Shift currentShift = shiftRepository.findByDateAndType(today, currentShiftType).orElse(null);

        // 4단계: 현재 근무조의 내 모든 배정 정보를 미리 조회 (한 번만)
        // [최적화 전] 각 환자마다 조회 → 환자 10명이면 10번 쿼리
        // [최적화 후] 모든 배정을 한 번에 조회 → 1번 쿼리
        // 쿼리 1회: SELECT * FROM assignment WHERE nurse_id = ? AND shift_id = ?
        List<Assignment> myAssignments = currentShift != null
            ? assignmentRepository.findByNurseIdAndShiftId(userId, currentShift.getId())
            : List.of();

        // 5단계: 환자 정보 + 최근 바이탈 조합
        List<MyPatientResponse> responses = patients.stream()
                .map(patient -> {
                    // 각 환자의 최근 바이탈 조회
                    // TODO: 성능 개선 여지 - 모든 환자의 바이탈을 IN 쿼리로 한 번에 조회 가능
                    // 현재: 환자마다 1번 쿼리 (환자 10명 = 10번 쿼리)
                    // 개선 후: 1번 쿼리로 모든 환자의 바이탈 조회 가능
                    List<VitalSign> vitals = vitalSignRepository.findByPatientIdOrderByMeasuredAtDesc(patient.getId());
                    VitalSign latestVital = vitals.isEmpty() ? null : vitals.get(0);

                    return MyPatientResponse.builder()
                            .patientId(patient.getId())
                            .chartNumber(patient.getChartNumber())
                            .name(patient.getName())
                            .age(patient.getAge())
                            .gender(patient.getGender())
                            .diagnosis(patient.getDiagnosis())
                            .allergies(patient.getAllergies())
                            .triageLevel(patient.getTriageLevel())
                            .isPrimary(false) // 주담당 기능 제거
                            .systolicBp(latestVital != null ? latestVital.getSystolicBp() : null)
                            .diastolicBp(latestVital != null ? latestVital.getDiastolicBp() : null)
                            .heartRate(latestVital != null ? latestVital.getHeartRate() : null)
                            .bodyTemp(latestVital != null ? latestVital.getBodyTemp() : null)
                            .respiratoryRate(latestVital != null ? latestVital.getRespiratoryRate() : null)
                            .spo2(latestVital != null ? latestVital.getSpo2() : null)
                            .lastVitalTime(latestVital != null ? latestVital.getMeasuredAt() : null)
                            .departmentName(patient.getDepartment() != null ? patient.getDepartment().getName() : null)
                            .departmentCode(patient.getDepartment() != null ? patient.getDepartment().getCode() : null)
                            .admissionDate(patient.getAdmissionDate())
                            .roomNumber(patient.getRoomNumber())
                            .bedNumber(patient.getBedNumber())
                            .attendingPhysician(patient.getAttendingPhysician())
                            .fallRisk(patient.getFallRisk())
                            .isolationType(patient.getIsolationType())
                            .build();
                })
                .collect(Collectors.toList());

        // 6단계: 응급실 환자는 트리아지 순서로 정렬
        // 트리아지 레벨: 1(최우선) → 5(비응급) 순서
        responses.sort((a, b) -> {
            // 트리아지가 있는 환자 우선
            if (a.getTriageLevel() != null && b.getTriageLevel() == null) return -1;
            if (a.getTriageLevel() == null && b.getTriageLevel() != null) return 1;
            if (a.getTriageLevel() != null && b.getTriageLevel() != null) {
                return a.getTriageLevel().compareTo(b.getTriageLevel());
            }
            return 0;
        });

        return responses;
    }

    /**
     * 부서 요약 정보 조회
     */
    public DepartmentSummaryResponse getDepartmentSummary(Long userId) {
        return getDepartmentSummary(userId, null, null);
    }

    public DepartmentSummaryResponse getDepartmentSummary(Long userId, LocalDate date, ShiftType shiftType) {
        // 사용자 조회
        User nurse = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (nurse.getDepartment() == null) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }

        // 부서 전체 환자 수
        Long totalPatients = (long) patientRepository.findByDepartmentIdAndIsAdmitted(
                nurse.getDepartment().getId(), true).size();

        // 내 담당 환자 수
        Long myPatients = (long) getMyPatients(userId, date, shiftType).size();

        // 현재 근무조
        ShiftType currentShiftType = shiftType == null ? getCurrentShiftType() : shiftType;
        String shiftTypeName = getShiftTypeName(currentShiftType);
        LocalDate today = date == null ? LocalDate.now() : date;
        ShiftType nextShiftType = switch (currentShiftType) {
            case DAY -> ShiftType.EVENING;
            case EVENING -> ShiftType.NIGHT;
            case NIGHT -> ShiftType.DAY;
        };
        Shift currentShift = shiftRepository.findByDateAndType(today, currentShiftType).orElse(null);
        Shift nextShift = shiftRepository.findByDateAndType(today, nextShiftType).orElse(null);
        // 근무조 ID는 날짜별로 다르며, 다음 시작 시간이 앞서면 다음 날짜의 근무조를 사용한다.
        if (currentShift != null && nextShift != null
                && !nextShift.getStartTime().isAfter(currentShift.getStartTime())) {
            nextShift = shiftRepository.findByDateAndType(today.plusDays(1), nextShiftType).orElse(null);
        }
        Long shiftId = currentShift == null ? null : currentShift.getId();
        Long nextShiftId = nextShift == null ? null : nextShift.getId();

        return DepartmentSummaryResponse.builder()
                .departmentId(nurse.getDepartment().getId())
                .shiftId(shiftId)
                .nextShiftId(nextShiftId)
                .departmentName(nurse.getDepartment().getName())
                .departmentCode(nurse.getDepartment().getCode().toUpperCase()) // 부서 코드 대문자 변환
                .totalPatients(totalPatients)
                .myPatients(myPatients)
                .nurseName(nurse.getName())
                .shiftType(shiftTypeName)
                .build();
    }

    /**
     * 현재 시간 기준 근무조 타입 반환
     */
    private ShiftType getCurrentShiftType() {
        return shiftTypeAt(LocalTime.now());
    }

    static ShiftType shiftTypeAt(LocalTime now) {
        
        if (!now.isBefore(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(16, 0))) {
            return ShiftType.DAY;
        } else if (!now.isBefore(LocalTime.of(16, 0))) {
            return ShiftType.EVENING;
        } else {
            return ShiftType.NIGHT;
        }
    }

    /**
     * ShiftType을 영문 이름으로 변환
     */
    private String getShiftTypeName(ShiftType shiftType) {
        return switch (shiftType) {
            case DAY -> "Day";
            case EVENING -> "Evening";
            case NIGHT -> "Night";
        };
    }

}
