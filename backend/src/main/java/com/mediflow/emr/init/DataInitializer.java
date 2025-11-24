package com.mediflow.emr.init;

import com.mediflow.emr.entity.*;
import com.mediflow.emr.entity.enums.*;
import com.mediflow.emr.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 개발 환경 초기 데이터 생성
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final ShiftRepository shiftRepository;
    private final AssignmentRepository assignmentRepository;
    private final VitalSignRepository vitalSignRepository;
    private final IntakeOutputRepository intakeOutputRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) {
        log.info("========================================");
        log.info("개발 환경 초기 데이터 생성 시작");
        log.info("========================================");

        // 1. 부서 생성
        List<DepartmentEntity> departments = createDepartments();
        log.info("✅ 부서 {} 개 생성 완료", departments.size());

        // 2. Admin 계정 생성
        User admin = createAdminUser();
        log.info("✅ Admin 계정 생성 완료: {}", admin.getEmail());

        // 3. 간호사 생성
        List<User> nurses = createNurses(departments);
        log.info("✅ 간호사 {} 명 생성 완료", nurses.size());

        // 4. 환자 생성
        List<Patient> patients = createPatients(departments);
        log.info("✅ 환자 {} 명 생성 완료", patients.size());

        // 5. 근무조 생성 (오늘 날짜 기준)
        List<Shift> shifts = createShifts();
        log.info("✅ 근무조 {} 개 생성 완료", shifts.size());

        // 6. 간호사-환자 배정 (모든 근무조)
        List<Assignment> assignments = createAssignments(nurses, patients, shifts);
        log.info("✅ 배정 {} 건 생성 완료", assignments.size());

        // 7. 바이탈 사인 생성
        List<VitalSign> vitals = createVitalSigns(nurses, patients);
        log.info("✅ 바이탈 사인 {} 건 생성 완료", vitals.size());

        // 8. 섭취배설량 생성
        List<IntakeOutput> ioRecords = createIntakeOutputs(nurses, patients);
        log.info("✅ 섭취배설량 {} 건 생성 완료", ioRecords.size());

        log.info("========================================");
        log.info("초기 데이터 생성 완료!");
        log.info("========================================");
    }

    /**
     * 부서 8개 생성
     */
    private List<DepartmentEntity> createDepartments() {
        List<DepartmentEntity> departments = new ArrayList<>();

        departments.add(DepartmentEntity.builder()
                .name("응급실")
                .code("ER")
                .type(Department.EMERGENCY)
                .bedCount(20)
                .build());

        departments.add(DepartmentEntity.builder()
                .name("중환자실")
                .code("ICU")
                .type(Department.ICU)
                .bedCount(15)
                .build());

        departments.add(DepartmentEntity.builder()
                .name("내과병동")
                .code("MW")
                .type(Department.INTERNAL_MEDICINE)
                .bedCount(40)
                .build());

        departments.add(DepartmentEntity.builder()
                .name("외과병동")
                .code("SW")
                .type(Department.SURGERY)
                .bedCount(35)
                .build());

        departments.add(DepartmentEntity.builder()
                .name("내과외래")
                .code("MO")
                .type(Department.INTERNAL_MEDICINE)
                .bedCount(null)
                .build());

        departments.add(DepartmentEntity.builder()
                .name("외과외래")
                .code("SO")
                .type(Department.SURGERY)
                .bedCount(null)
                .build());

        departments.add(DepartmentEntity.builder()
                .name("수술실")
                .code("OR")
                .type(Department.OR)
                .bedCount(8)
                .build());

        departments.add(DepartmentEntity.builder()
                .name("투석실")
                .code("HD")
                .type(Department.INTERNAL_MEDICINE)
                .bedCount(12)
                .build());

        return departmentRepository.saveAll(departments);
    }

    /**
     * Admin 계정 생성
     */
    private User createAdminUser() {
        return userRepository.save(User.builder()
                .email("admin@mediflow.com")
                .password(passwordEncoder.encode("admin123@"))
                .nickname("관리자")
                .name("시스템 관리자")
                .provider(Provider.LOCAL)
                .providerId("admin")
                .role(Role.ADMIN)
                .isActive(true)
                .isLocked(false)
                .emailVerified(true)
                .build());
    }

    /**
     * 간호사 생성 (부서별 인원 배치)
     * 응급실 5명, 중환자실 4명, 내과병동 6명, 외과병동 5명,
     * 내과외래 3명, 외과외래 3명, 수술실 4명, 투석실 3명
     */
    private List<User> createNurses(List<DepartmentEntity> departments) {
        List<User> nurses = new ArrayList<>();
        int[] nurseCounts = {5, 4, 6, 5, 3, 3, 4, 3}; // 부서별 간호사 수

        for (int i = 0; i < departments.size(); i++) {
            DepartmentEntity dept = departments.get(i);
            int count = nurseCounts[i];

            for (int j = 1; j <= count; j++) {
                nurses.add(User.builder()
                        .email(String.format("nurse%s%d@mediflow.com", dept.getCode().toLowerCase(), j))
                        .password(passwordEncoder.encode("nurse123@"))
                        .nickname(String.format("%s 간호사%d", dept.getName(), j))
                        .name(String.format("간호사%s%d", dept.getCode(), j))
                        .phone(String.format("010-%04d-%04d", random.nextInt(10000), random.nextInt(10000)))
                        .hireDate(LocalDate.now().minusYears(random.nextInt(10)))
                        .provider(Provider.LOCAL)
                        .providerId(String.format("nurse_%s_%d", dept.getCode(), j))
                        .role(Role.NURSE)
                        .department(dept)
                        .isActive(true)
                        .isLocked(false)
                        .emailVerified(true)
                        .build());
            }
        }

        return userRepository.saveAll(nurses);
    }

    /**
     * 환자 생성 (부서별 환자 수)
     * 응급실 15명, 중환자실 12명, 내과병동 35명, 외과병동 30명,
     * 내과외래 20명, 외과외래 18명, 수술실 6명, 투석실 10명
     */
    private List<Patient> createPatients(List<DepartmentEntity> departments) {
        List<Patient> patients = new ArrayList<>();
        int[] patientCounts = {15, 12, 35, 30, 20, 18, 6, 10}; // 부서별 환자 수
        String[] diagnoses = {
                "급성 심근경색", "뇌졸중", "폐렴", "당뇨병", "고혈압",
                "골절", "맹장염", "위염", "천식", "신부전"
        };

        int chartNumberCounter = 1000;

        for (int i = 0; i < departments.size(); i++) {
            DepartmentEntity dept = departments.get(i);
            int count = patientCounts[i];
            boolean isOutpatientOrOR = dept.getCode().equals("MO") || 
                                       dept.getCode().equals("SO") || 
                                       dept.getCode().equals("OR");

            for (int j = 1; j <= count; j++) {
                chartNumberCounter++;
                int age = 20 + random.nextInt(60);
                
                Patient.PatientBuilder builder = Patient.builder()
                        .chartNumber(String.format("C%06d", chartNumberCounter))
                        .name(String.format("환자%d", chartNumberCounter))
                        .age(age)
                        .gender(random.nextBoolean() ? Gender.M : Gender.F)
                        .ssn(String.format("%02d%02d%02d1", 
                                age % 100, 
                                1 + random.nextInt(12), 
                                1 + random.nextInt(28)))
                        .diagnosis(diagnoses[random.nextInt(diagnoses.length)])
                        .admissionDate(LocalDate.now().minusDays(random.nextInt(30)))
                        .bloodType(getRandomBloodType())
                        .allergies(random.nextBoolean() ? "없음" : "페니실린")
                        .guardianName(String.format("보호자%d", chartNumberCounter))
                        .guardianPhone(String.format("010-%04d-%04d", 
                                random.nextInt(10000), 
                                random.nextInt(10000)))
                        .isAdmitted(!isOutpatientOrOR)
                        .department(dept);

                // 응급실 환자는 triage_level 설정
                if (dept.getCode().equals("ER")) {
                    builder.triageLevel(1 + random.nextInt(5)); // 1-5
                }

                // 외래/수술실이 아니면 퇴원일 설정 가능
                if (!isOutpatientOrOR && random.nextBoolean()) {
                    builder.dischargeDate(LocalDate.now().plusDays(random.nextInt(7)));
                }

                patients.add(builder.build());
            }
        }

        return patientRepository.saveAll(patients);
    }

    /**
     * 근무조 생성 (오늘 날짜 기준 3교대)
     */
    private List<Shift> createShifts() {
        List<Shift> shifts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 주간조 (08:00-16:00)
        shifts.add(Shift.builder()
                .date(today)
                .type(ShiftType.DAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(16, 0))
                .build());

        // 초번 (16:00-00:00)
        shifts.add(Shift.builder()
                .date(today)
                .type(ShiftType.EVENING)
                .startTime(LocalTime.of(16, 0))
                .endTime(LocalTime.of(0, 0))
                .build());

        // 야간조 (00:00-08:00)
        shifts.add(Shift.builder()
                .date(today)
                .type(ShiftType.NIGHT)
                .startTime(LocalTime.of(0, 0))
                .endTime(LocalTime.of(8, 0))
                .build());

        return shiftRepository.saveAll(shifts);
    }

    /**
     * 간호사-환자 배정 (모든 근무조에 배정)
     */
    private List<Assignment> createAssignments(List<User> nurses, 
                                                 List<Patient> patients, 
                                                 List<Shift> shifts) {
        List<Assignment> assignments = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 각 근무조별로 배정
        for (Shift shift : shifts) {
            // 부서별로 간호사와 환자를 그룹화하여 배정
            for (User nurse : nurses) {
                if (nurse.getDepartment() == null) continue;

                // 같은 부서의 입원 환자만 필터링
                List<Patient> deptPatients = patients.stream()
                        .filter(p -> p.getDepartment() != null)
                        .filter(p -> p.getDepartment().getId().equals(nurse.getDepartment().getId()))
                        .filter(Patient::getIsAdmitted)
                        .toList();

                if (deptPatients.isEmpty()) continue;

                // 간호사당 2-4명 환자 배정
                int patientsPerNurse = 2 + random.nextInt(3);
                int startIdx = (assignments.size() + shift.getId().intValue()) % deptPatients.size();

                for (int i = 0; i < Math.min(patientsPerNurse, deptPatients.size()); i++) {
                    int patientIdx = (startIdx + i) % deptPatients.size();
                    Patient patient = deptPatients.get(patientIdx);

                    assignments.add(Assignment.builder()
                            .nurse(nurse)
                            .patient(patient)
                            .shift(shift)
                            .assignedDate(today)
                            .isPrimary(i == 0) // 첫 번째 환자는 주담당
                            .build());
                }
            }
        }

        return assignmentRepository.saveAll(assignments);
    }

    /**
     * 바이탈 사인 생성 (입원 환자 대상, 최근 3일간 데이터)
     */
    private List<VitalSign> createVitalSigns(List<User> nurses, List<Patient> patients) {
        List<VitalSign> vitals = new ArrayList<>();
        
        // 입원 환자만 필터링
        List<Patient> admittedPatients = patients.stream()
                .filter(Patient::getIsAdmitted)
                .toList();

        for (Patient patient : admittedPatients) {
            // 같은 부서의 간호사 찾기
            List<User> deptNurses = nurses.stream()
                    .filter(n -> n.getDepartment() != null)
                    .filter(n -> patient.getDepartment() != null)
                    .filter(n -> n.getDepartment().getId().equals(patient.getDepartment().getId()))
                    .toList();

            if (deptNurses.isEmpty()) continue;

            // 과거 3일간 하루 2-4회 바이탈 측정 (30분 단위)
            for (int day = 1; day <= 3; day++) {
                int measurementsPerDay = 2 + random.nextInt(3);
                
                for (int i = 0; i < measurementsPerDay; i++) {
                    User nurse = deptNurses.get(random.nextInt(deptNurses.size()));
                    
                    // 측정 시간 (과거 day일, 8시~20시 사이, 30분 단위)
                    int hour = 8 + random.nextInt(13); // 8~20시
                    int minute = random.nextBoolean() ? 0 : 30; // 0분 또는 30분
                    
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    java.time.LocalDateTime measuredAt = now
                            .minusDays(day)
                            .withHour(hour)
                            .withMinute(minute)
                            .withSecond(0)
                            .withNano(0);

                    // 미래 시간이면 하루 더 과거로 (안전장치)
                    if (measuredAt.isAfter(now)) {
                        measuredAt = measuredAt.minusDays(1);
                    }

                    // 정상 범위 기준으로 랜덤 바이탈 생성
                    // 체온: 36.0 ~ 37.5°C (소수점 한자리)
                    double bodyTemp = 36.0 + (random.nextInt(16) / 10.0); // 36.0, 36.1, 36.2, ..., 37.5

                    vitals.add(VitalSign.builder()
                            .patient(patient)
                            .nurse(nurse)
                            .systolicBp(110 + random.nextInt(30))  // 110-140
                            .diastolicBp(70 + random.nextInt(20))  // 70-90
                            .heartRate(65 + random.nextInt(30))    // 65-95
                            .bodyTemp(bodyTemp)  // 36.0-37.5 (소수점 한자리)
                            .respiratoryRate(14 + random.nextInt(6))  // 14-20
                            .spo2(96 + random.nextInt(5))  // 96-100
                            .measuredAt(measuredAt)
                            .build());
                }
            }
        }

        return vitalSignRepository.saveAll(vitals);
    }

    /**
     * 섭취배설량 생성 (입원 환자 대상, 과거 3일간 데이터)
     */
    private List<IntakeOutput> createIntakeOutputs(List<User> nurses, List<Patient> patients) {
        List<IntakeOutput> ioRecords = new ArrayList<>();
        
        // 입원 환자만 필터링
        List<Patient> admittedPatients = patients.stream()
                .filter(Patient::getIsAdmitted)
                .toList();

        for (Patient patient : admittedPatients) {
            // 같은 부서의 간호사 찾기
            List<User> deptNurses = nurses.stream()
                    .filter(n -> n.getDepartment() != null)
                    .filter(n -> patient.getDepartment() != null)
                    .filter(n -> n.getDepartment().getId().equals(patient.getDepartment().getId()))
                    .toList();

            if (deptNurses.isEmpty()) continue;

            // 과거 3일간 하루 3-5회 I/O 기록 (4시간 간격)
            for (int day = 1; day <= 3; day++) {
                int recordsPerDay = 3 + random.nextInt(3); // 3-5회
                
                for (int i = 0; i < recordsPerDay; i++) {
                    User nurse = deptNurses.get(random.nextInt(deptNurses.size()));
                    
                    // 기록 시간 (과거 day일, 4시간 간격: 8시, 12시, 16시, 20시, 24시)
                    int[] hours = {8, 12, 16, 20, 0};
                    int hourIndex = i % hours.length;
                    int hour = hours[hourIndex];
                    
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    java.time.LocalDateTime recordedAt = now
                            .minusDays(day)
                            .withHour(hour)
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);

                    // 미래 시간이면 하루 더 과거로
                    if (recordedAt.isAfter(now)) {
                        recordedAt = recordedAt.minusDays(1);
                    }

                    // 정상 범위 기준으로 랜덤 I/O 생성
                    // 섭취: 경구 100-500mL, 정맥 500-1500mL
                    // 배설: 소변 200-400mL, 배액 0-100mL
                    int intakeOral = 100 + random.nextInt(401);  // 100-500
                    int intakeIv = 500 + random.nextInt(1001);   // 500-1500
                    int outputUrine = 200 + random.nextInt(201); // 200-400
                    int outputDrain = random.nextBoolean() ? random.nextInt(101) : 0; // 0-100 또는 0

                    ioRecords.add(IntakeOutput.builder()
                            .patient(patient)
                            .nurse(nurse)
                            .intakeOral(intakeOral)
                            .intakeIv(intakeIv)
                            .outputUrine(outputUrine)
                            .outputDrain(outputDrain)
                            .recordedAt(recordedAt)
                            .build());
                }
            }
        }

        return intakeOutputRepository.saveAll(ioRecords);
    }

    /**
     * 랜덤 혈액형 생성
     */
    private String getRandomBloodType() {
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        return bloodTypes[random.nextInt(bloodTypes.length)];
    }
}
