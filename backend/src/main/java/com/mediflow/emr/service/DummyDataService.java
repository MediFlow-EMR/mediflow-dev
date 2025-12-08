package com.mediflow.emr.service;

import com.mediflow.emr.entity.*;
import com.mediflow.emr.entity.enums.Role;
import com.mediflow.emr.entity.enums.ShiftType;
import com.mediflow.emr.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 신규 회원가입 사용자에게 더미 데이터를 적용하는 서비스
 * DataInitializer에서 생성한 기존 더미 데이터(부서, 환자, 근무조)를 활용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DummyDataService {

    private final DepartmentRepository departmentRepository;
    private final PatientRepository patientRepository;
    private final ShiftRepository shiftRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    private final Random random = new Random();

    /**
     * 신규 사용자에게 더미 데이터 적용
     * - 부서 배정 (응급실 또는 중환자실)
     * - 간호사 역할 부여
     * - 환자 배정 (해당 부서의 환자 2-4명)
     *
     * @param user 신규 회원가입한 사용자
     */
    @Transactional
    public void applyDummyDataToNewUser(User user) {
        try {
            log.info("신규 사용자에게 더미 데이터 적용 시작 - userId: {}, email: {}", user.getId(), user.getEmail());

            // 1. 부서 데이터가 없으면 적용 불가
            long deptCount = departmentRepository.count();
            if (deptCount == 0) {
                log.warn("부서 데이터가 없어서 더미 데이터를 적용할 수 없습니다. DataInitializer를 먼저 실행하세요.");
                return;
            }

            // 2. 부서 배정 (응급실 또는 중환자실 중 랜덤)
            DepartmentEntity department = assignRandomDepartment();
            if (department == null) {
                log.warn("부서 배정 실패 - 응급실 또는 중환자실을 찾을 수 없습니다.");
                return;
            }

            // 3. 사용자 정보 업데이트 (부서, 역할, 활성화, 이메일 인증)
            user.updateDepartment(department);
            user.updateRole(Role.NURSE);
            user.activate();
            user.unlock();
            user.completeVerifying();
            user.updateHireDate(LocalDate.now());

            // 이름이 없으면 닉네임으로 설정
            if (user.getName() == null || user.getName().isBlank()) {
                String name = user.getNickname() != null ? user.getNickname() : "신규 간호사";
                user.updateName(name);
            }

            userRepository.save(user);
            log.info("사용자 정보 업데이트 완료 - userId: {}, department: {}, role: NURSE",
                    user.getId(), department.getName());

            // 4. 오늘 날짜의 근무조 조회 (없으면 생성)
            LocalDate today = LocalDate.now();
            List<Shift> todayShifts = shiftRepository.findByDate(today);

            if (todayShifts.isEmpty()) {
                log.info("오늘 날짜({})의 근무조가 없습니다. 근무조를 생성합니다.", today);
                todayShifts = createTodayShifts(today);
                if (todayShifts.isEmpty()) {
                    log.warn("근무조 생성 실패. 배정을 건너뜁니다.");
                    return;
                }
            }

            // 5. 해당 부서의 입원 환자 조회
            List<Patient> deptPatients = patientRepository.findByDepartmentAndIsAdmitted(department, true);

            if (deptPatients.isEmpty()) {
                log.info("부서에 입원 환자가 없습니다. 배정을 건너뜁니다.");
                return;
            }

            // 6. 각 근무조별로 환자 배정 (2-4명)
            int assignmentCount = 0;
            for (Shift shift : todayShifts) {
                int patientsPerNurse = 2 + random.nextInt(3); // 2-4명

                for (int i = 0; i < Math.min(patientsPerNurse, deptPatients.size()); i++) {
                    Patient patient = deptPatients.get(random.nextInt(deptPatients.size()));

                    // 중복 배정 체크
                    boolean alreadyAssigned = assignmentRepository.existsByNurseAndPatientAndShift(user, patient, shift);
                    if (alreadyAssigned) {
                        continue;
                    }

                    Assignment assignment = Assignment.builder()
                            .nurse(user)
                            .patient(patient)
                            .shift(shift)
                            .assignedDate(today)
                            .isPrimary(false) // 주담당 기능 제거
                            .build();

                    assignmentRepository.save(assignment);
                    assignmentCount++;
                }
            }

            log.info("신규 사용자에게 더미 데이터 적용 완료 - userId: {}, 배정 수: {}",
                    user.getId(), assignmentCount);

        } catch (Exception e) {
            log.error("더미 데이터 적용 중 오류 발생 - userId: {}", user.getId(), e);
            // 오류가 발생해도 회원가입은 계속 진행
        }
    }

    /**
     * 오늘 날짜의 근무조 생성 (DAY, EVENING, NIGHT)
     */
    private List<Shift> createTodayShifts(LocalDate today) {
        List<Shift> shifts = new ArrayList<>();

        shifts.add(Shift.builder()
                .type(ShiftType.DAY)
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(15, 0))
                .date(today)
                .build());

        shifts.add(Shift.builder()
                .type(ShiftType.EVENING)
                .startTime(LocalTime.of(15, 0))
                .endTime(LocalTime.of(23, 0))
                .date(today)
                .build());

        shifts.add(Shift.builder()
                .type(ShiftType.NIGHT)
                .startTime(LocalTime.of(23, 0))
                .endTime(LocalTime.of(7, 0))
                .date(today)
                .build());

        return shiftRepository.saveAll(shifts);
    }

    /**
     * 응급실 또는 중환자실 중 랜덤으로 배정
     */
    private DepartmentEntity assignRandomDepartment() {
        // 응급실 조회
        DepartmentEntity er = departmentRepository.findByCode("ER").orElse(null);
        // 중환자실 조회
        DepartmentEntity icu = departmentRepository.findByCode("ICU").orElse(null);

        // 둘 다 있으면 랜덤 선택
        if (er != null && icu != null) {
            return random.nextBoolean() ? er : icu;
        }

        // 하나만 있으면 그것을 반환
        if (er != null) {
            return er;
        }
        if (icu != null) {
            return icu;
        }

        // 둘 다 없으면 첫 번째 부서 반환
        return departmentRepository.findAll().stream().findFirst().orElse(null);
    }
}

