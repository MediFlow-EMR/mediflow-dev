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
@Profile({"dev", "prod"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final ShiftRepository shiftRepository;
    private final AssignmentRepository assignmentRepository;
    private final VitalSignRepository vitalSignRepository;
    private final IntakeOutputRepository intakeOutputRepository;
    private final MedicalOrderRepository medicalOrderRepository;
    private final MedicationRepository medicationRepository;
    private final NursingNoteRepository nursingNoteRepository;
    private final TestResultRepository testResultRepository;
    private final HandoverRepository handoverRepository;
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

        // 9. 의료 오더 생성
        List<MedicalOrder> orders = createMedicalOrders(nurses, patients);
        log.info("✅ 의료 오더 {} 건 생성 완료", orders.size());

        // 10. 투약 기록 생성
        List<Medication> medications = createMedications(nurses, patients);
        log.info("✅ 투약 기록 {} 건 생성 완료", medications.size());

        // 11. 간호기록 생성
        List<NursingNote> nursingNotes = createNursingNotes(nurses, patients);
        log.info("✅ 간호기록 {} 건 생성 완료", nursingNotes.size());

        // 12. 검사 결과 생성
        List<TestResult> testResults = createTestResults(nurses, patients);
        log.info("✅ 검사 결과 {} 건 생성 완료", testResults.size());

        // 13. 인수인계 생성
        List<Handover> handovers = createHandovers(departments, shifts, nurses);
        log.info("✅ 인수인계 {} 건 생성 완료", handovers.size());

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

    /**
     * 의료 오더 생성 (입원 환자 대상)
     */
    private List<MedicalOrder> createMedicalOrders(List<User> nurses, List<Patient> patients) {
        List<MedicalOrder> orders = new ArrayList<>();
        
        // 입원 환자만 필터링
        List<Patient> admittedPatients = patients.stream()
                .filter(Patient::getIsAdmitted)
                .toList();

        String[] doctors = {"김철수", "이영희", "박민수", "정수진", "최동욱"};
        
        // 투약 오더 샘플 (식약처 API에서 검색 가능한 실제 약품명)
        String[][] medications = {
                {"타이레놀", null, "500mg", "PO", "1일 3회"},
                {"아스피린", null, "100mg", "PO", "1일 1회"},
                {"게보린", null, "1정", "PO", "1일 3회"},
                {"판피린", null, "1정", "PO", "1일 3회"},
                {"부루펜", null, "200mg", "PO", "1일 3회"},
                {"후시딘", null, "적당량", "TOPICAL", "1일 2회"},
                {"베아제", null, "1정", "PO", "1일 3회"},
                {"가스활명수", null, "1포", "PO", "필요시"}
        };

        for (Patient patient : admittedPatients) {
            // 환자당 2-4개 오더 생성
            int orderCount = 2 + random.nextInt(3);
            
            for (int i = 0; i < orderCount; i++) {
                String[] med = medications[random.nextInt(medications.length)];
                String doctor = doctors[random.nextInt(doctors.length)];
                
                // 오더 시간 (과거 1-3일)
                java.time.LocalDateTime orderedAt = java.time.LocalDateTime.now()
                        .minusDays(1 + random.nextInt(3))
                        .withHour(8 + random.nextInt(12))
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0);

                // 상태: 70% PENDING, 20% IN_PROGRESS, 10% COMPLETED
                String status;
                int statusRand = random.nextInt(10);
                if (statusRand < 7) {
                    status = "PENDING";
                } else if (statusRand < 9) {
                    status = "IN_PROGRESS";
                } else {
                    status = "COMPLETED";
                }

                orders.add(MedicalOrder.builder()
                        .patient(patient)
                        .orderType("MEDICATION")
                        .orderName(med[0])
                        .orderCode(med[1])
                        .dose(med[2])
                        .route(med[3])
                        .frequency(med[4])
                        .instructions("식후 30분")
                        .status(status)
                        .orderedAt(orderedAt)
                        .orderDoctor(doctor)
                        .build());
            }
        }

        return medicalOrderRepository.saveAll(orders);
    }

    /**
     * 투약 기록 생성 (입원 환자 대상, 과거 2일간)
     */
    private List<Medication> createMedications(List<User> nurses, List<Patient> patients) {
        List<Medication> medications = new ArrayList<>();
        
        // 입원 환자만 필터링
        List<Patient> admittedPatients = patients.stream()
                .filter(Patient::getIsAdmitted)
                .toList();

        String[] doctors = {"김철수", "이영희", "박민수", "정수진", "최동욱"};
        
        // 투약 샘플 (식약처 API에서 검색 가능한 실제 약품명)
        Object[][] drugs = {
                {"타이레놀", null, "500mg", MedicationRoute.PO, "1일 3회"},
                {"아스피린", null, "100mg", MedicationRoute.PO, "1일 1회"},
                {"게보린", null, "1정", MedicationRoute.PO, "1일 3회"},
                {"판피린", null, "1정", MedicationRoute.PO, "1일 3회"},
                {"부루펜", null, "200mg", MedicationRoute.PO, "1일 3회"},
                {"후시딘", null, "적당량", MedicationRoute.TOPICAL, "1일 2회"},
                {"베아제", null, "1정", MedicationRoute.PO, "1일 3회"},
                {"가스활명수", null, "1포", MedicationRoute.PO, "필요시"}
        };

        for (Patient patient : admittedPatients) {
            // 같은 부서의 간호사 찾기
            List<User> deptNurses = nurses.stream()
                    .filter(n -> n.getDepartment() != null)
                    .filter(n -> patient.getDepartment() != null)
                    .filter(n -> n.getDepartment().getId().equals(patient.getDepartment().getId()))
                    .toList();

            if (deptNurses.isEmpty()) continue;

            // 과거 2일간 하루 2-3회 투약
            for (int day = 1; day <= 2; day++) {
                int medsPerDay = 2 + random.nextInt(2);
                
                for (int i = 0; i < medsPerDay; i++) {
                    User nurse = deptNurses.get(random.nextInt(deptNurses.size()));
                    Object[] drug = drugs[random.nextInt(drugs.length)];
                    String doctor = doctors[random.nextInt(doctors.length)];
                    
                    // 투약 시간 (과거 day일, 8시/14시/20시)
                    int[] hours = {8, 14, 20};
                    int hour = hours[i % hours.length];
                    
                    java.time.LocalDateTime administeredAt = java.time.LocalDateTime.now()
                            .minusDays(day)
                            .withHour(hour)
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);

                    medications.add(Medication.builder()
                            .patient(patient)
                            .nurse(nurse)
                            .drugName((String) drug[0])
                            .drugCode((String) drug[1])
                            .dose((String) drug[2])
                            .route((MedicationRoute) drug[3])
                            .frequency((String) drug[4])
                            .administeredAt(administeredAt)
                            .orderDoctor(doctor)
                            .build());
                }
            }
        }

        return medicationRepository.saveAll(medications);
    }

    /**
     * 간호기록 생성 (입원 환자 대상, 과거 3일간)
     */
    private List<NursingNote> createNursingNotes(List<User> nurses, List<Patient> patients) {
        List<NursingNote> nursingNotes = new ArrayList<>();
        
        // 입원 환자만 필터링
        List<Patient> admittedPatients = patients.stream()
                .filter(Patient::getIsAdmitted)
                .toList();

        String[] noteTemplates = {
                "환자 상태 양호. 활력징후 안정적. 특이사항 없음.",
                "통증 호소하여 진통제 투여함. 통증 완화됨.",
                "식사 50% 섭취. 수분 섭취 권장함.",
                "보호자 면회. 환자 상태 설명함.",
                "수면 양호. 야간 특이사항 없음.",
                "배뇨 정상. 배변 1회 있음.",
                "활동 제한 중. 침상 안정 유지.",
                "드레싱 교환함. 상처 치유 양호.",
                "검사 결과 확인. 주치의 보고 완료.",
                "낙상 위험 평가 실시. 안전 교육함."
        };

        NoteCategory[] categories = {
                NoteCategory.OBSERVATION,
                NoteCategory.TREATMENT,
                NoteCategory.EDUCATION,
                NoteCategory.CONSULTATION,
                NoteCategory.MEDICATION
        };

        for (Patient patient : admittedPatients) {
            // 같은 부서의 간호사 찾기
            List<User> deptNurses = nurses.stream()
                    .filter(n -> n.getDepartment() != null)
                    .filter(n -> patient.getDepartment() != null)
                    .filter(n -> n.getDepartment().getId().equals(patient.getDepartment().getId()))
                    .toList();

            if (deptNurses.isEmpty()) continue;

            // 과거 3일간 하루 2-4회 간호기록
            for (int day = 1; day <= 3; day++) {
                int notesPerDay = 2 + random.nextInt(3); // 2-4회
                
                for (int i = 0; i < notesPerDay; i++) {
                    User nurse = deptNurses.get(random.nextInt(deptNurses.size()));
                    String noteContent = noteTemplates[random.nextInt(noteTemplates.length)];
                    NoteCategory category = categories[random.nextInt(categories.length)];

                    nursingNotes.add(NursingNote.builder()
                            .patient(patient)
                            .nurse(nurse)
                            .content(noteContent)
                            .plainText(noteContent)
                            .category(category)
                            .isImportant(random.nextInt(10) < 2) // 20% 확률로 중요 표시
                            .aiSuggested(false)
                            .build());
                }
            }
        }

        return nursingNoteRepository.saveAll(nursingNotes);
    }

    /**
     * 검사 결과 생성 (입원 환자 대상, 과거 7일간)
     */
    private List<TestResult> createTestResults(List<User> nurses, List<Patient> patients) {
        List<TestResult> testResults = new ArrayList<>();
        
        // 입원 환자만 필터링
        List<Patient> admittedPatients = patients.stream()
                .filter(Patient::getIsAdmitted)
                .toList();

        // 혈액검사 항목
        Object[][] bloodTests = {
                {"WBC", "4.0-10.0", "x10³/μL"},
                {"RBC", "4.2-5.4", "x10⁶/μL"},
                {"Hemoglobin", "12.0-16.0", "g/dL"},
                {"Platelet", "150-400", "x10³/μL"},
                {"Glucose", "70-100", "mg/dL"},
                {"Creatinine", "0.6-1.2", "mg/dL"}
        };

        // 소변검사 항목
        Object[][] urineTests = {
                {"pH", "5.0-8.0", ""},
                {"Protein", "Negative", ""},
                {"Glucose", "Negative", ""},
                {"RBC", "0-2", "/HPF"}
        };

        // 영상검사 판독소견
        String[] imagingResults = {
                "정상 소견. 특이사항 없음.",
                "경미한 염증 소견 관찰됨.",
                "이전 검사 대비 호전 양상.",
                "추적 관찰 필요.",
                "정상 범위 내 소견."
        };

        for (Patient patient : admittedPatients) {
            // 같은 부서의 간호사 찾기
            List<User> deptNurses = nurses.stream()
                    .filter(n -> n.getDepartment() != null)
                    .filter(n -> patient.getDepartment() != null)
                    .filter(n -> n.getDepartment().getId().equals(patient.getDepartment().getId()))
                    .toList();

            if (deptNurses.isEmpty()) continue;

            // 과거 7일간 검사 결과 생성
            for (int day = 1; day <= 7; day++) {
                User nurse = deptNurses.get(random.nextInt(deptNurses.size()));
                LocalDate testDate = LocalDate.now().minusDays(day);
                java.time.LocalDateTime resultDate = testDate.atTime(14, 0).plusHours(random.nextInt(4));

                // 혈액검사 (3일에 1번)
                if (day % 3 == 0) {
                    Object[] test = bloodTests[random.nextInt(bloodTests.length)];
                    String testName = (String) test[0];
                    String refRange = (String) test[1];
                    String unit = (String) test[2];
                    
                    // 정상 범위 내 값 생성
                    boolean isAbnormal = random.nextInt(10) < 2; // 20% 확률로 이상
                    String resultValue = generateTestValue(testName, refRange, unit, isAbnormal);

                    testResults.add(TestResult.builder()
                            .patient(patient)
                            .nurse(nurse)
                            .testType(TestType.BLOOD)
                            .testName(testName)
                            .resultValue(resultValue)
                            .referenceRange(refRange + " " + unit)
                            .isAbnormal(isAbnormal)
                            .testDate(testDate)
                            .resultDate(resultDate)
                            .status(TestStatus.COMPLETED)
                            .build());
                }

                // 소변검사 (5일에 1번)
                if (day % 5 == 0) {
                    Object[] test = urineTests[random.nextInt(urineTests.length)];
                    String testName = (String) test[0];
                    String refRange = (String) test[1];
                    String unit = (String) test[2];
                    
                    boolean isAbnormal = random.nextInt(10) < 1; // 10% 확률로 이상
                    String resultValue = generateTestValue(testName, refRange, unit, isAbnormal);

                    testResults.add(TestResult.builder()
                            .patient(patient)
                            .nurse(nurse)
                            .testType(TestType.URINE)
                            .testName(testName)
                            .resultValue(resultValue)
                            .referenceRange(refRange + " " + unit)
                            .isAbnormal(isAbnormal)
                            .testDate(testDate)
                            .resultDate(resultDate)
                            .status(TestStatus.COMPLETED)
                            .build());
                }

                // 영상검사 (7일에 1번)
                if (day == 7) {
                    TestType[] imagingTypes = {TestType.XRAY, TestType.CT, TestType.ULTRASOUND};
                    TestType imagingType = imagingTypes[random.nextInt(imagingTypes.length)];
                    String imagingName = switch (imagingType) {
                        case XRAY -> "Chest X-Ray";
                        case CT -> "Abdomen CT";
                        case ULTRASOUND -> "Abdomen Ultrasound";
                        default -> "Imaging";
                    };

                    testResults.add(TestResult.builder()
                            .patient(patient)
                            .nurse(nurse)
                            .testType(imagingType)
                            .testName(imagingName)
                            .resultValue(imagingResults[random.nextInt(imagingResults.length)])
                            .referenceRange(null)
                            .isAbnormal(false)
                            .testDate(testDate)
                            .resultDate(resultDate)
                            .status(TestStatus.COMPLETED)
                            .build());
                }
            }
        }

        return testResultRepository.saveAll(testResults);
    }

    /**
     * 검사 결과값 생성
     */
    private String generateTestValue(String testName, String refRange, String unit, boolean isAbnormal) {
        if (refRange.contains("-")) {
            // 숫자 범위
            String[] range = refRange.split("-");
            try {
                double min = Double.parseDouble(range[0].trim());
                double max = Double.parseDouble(range[1].trim());
                double value;
                
                if (isAbnormal) {
                    // 이상값: 범위 밖
                    value = random.nextBoolean() ? min * 0.7 : max * 1.3;
                } else {
                    // 정상값: 범위 내
                    value = min + (max - min) * random.nextDouble();
                }
                
                return String.format("%.1f %s", value, unit);
            } catch (NumberFormatException e) {
                return refRange;
            }
        } else if (refRange.equals("Negative")) {
            return isAbnormal ? "Positive" : "Negative";
        } else {
            return refRange;
        }
    }

    /**
     * 인수인계 생성 (과거 5일간, 부서별 근무조 교대마다)
     */
    private List<Handover> createHandovers(List<DepartmentEntity> departments, 
                                            List<Shift> shifts, 
                                            List<User> nurses) {
        List<Handover> handovers = new ArrayList<>();
        
        // 응급실과 중환자실만 인수인계 생성 (입원 부서)
        List<DepartmentEntity> targetDepts = departments.stream()
                .filter(d -> d.getCode().equals("ER") || d.getCode().equals("ICU") || 
                            d.getCode().equals("MW") || d.getCode().equals("SW"))
                .toList();

        String[] summaryTemplates = {
                "[환자001 (C001001, 45세/남)]\n- 주요 변화: 활력징후 안정적, 통증 완화됨\n- 수행한 처치: 진통제 투여, 드레싱 교환\n- 지속 관찰 사항: 수술 후 상태 모니터링 필요\n\n[환자002 (C001002, 62세/여)]\n- 주요 변화: 혈압 상승 경향, 식사 섭취량 증가\n- 수행한 처치: 혈압약 투여, 바이탈 체크\n- 지속 관찰 사항: 혈압 변화 추이 관찰",
                
                "[환자003 (C001003, 38세/남)]\n- 주요 변화: 발열 38.2도, 기침 증상 지속\n- 수행한 처치: 해열제 투여, 수액 공급\n- 지속 관찰 사항: 체온 변화 모니터링, 호흡 상태 관찰\n\n[환자004 (C001004, 55세/여)]\n- 주요 변화: 수면 양호, 통증 호소 없음\n- 수행한 처치: 정기 투약, 활력징후 측정\n- 지속 관찰 사항: 특이사항 없음",
                
                "[환자005 (C001005, 71세/남)]\n- 주요 변화: 의식 명료, 식사 70% 섭취\n- 수행한 처치: 인슐린 투여, 혈당 체크\n- 지속 관찰 사항: 혈당 조절 상태 확인\n\n[환자006 (C001006, 49세/여)]\n- 주요 변화: 배액량 감소, 상처 치유 양호\n- 수행한 처치: 드레싱 교환, 항생제 투여\n- 지속 관찰 사항: 감염 징후 관찰",
                
                "[환자007 (C001007, 33세/남)]\n- 주요 변화: 통증 VAS 7→4로 감소\n- 수행한 처치: 진통제 투여, 냉찜질 적용\n- 지속 관찰 사항: 통증 재평가 필요\n\n[환자008 (C001008, 58세/여)]\n- 주요 변화: 호흡곤란 호소, SpO2 92%\n- 수행한 처치: 산소 공급 2L/min, 주치의 보고\n- 지속 관찰 사항: 호흡 상태 집중 관찰 필요",
                
                "[환자009 (C001009, 66세/남)]\n- 주요 변화: 배뇨 정상, 부종 감소\n- 수행한 처치: 이뇨제 투여, I/O 체크\n- 지속 관찰 사항: 수분 균형 모니터링\n\n[환자010 (C001010, 42세/여)]\n- 주요 변화: 오심 증상 완화, 식사 가능\n- 수행한 처치: 항구토제 투여, 식이 조절\n- 지속 관찰 사항: 소화 상태 관찰"
        };

        // 과거 5일간 데이터 생성
        for (int day = 1; day <= 5; day++) {
            LocalDate handoverDate = LocalDate.now().minusDays(day);
            
            for (DepartmentEntity dept : targetDepts) {
                // 해당 부서의 간호사 찾기
                List<User> deptNurses = nurses.stream()
                        .filter(n -> n.getDepartment() != null)
                        .filter(n -> n.getDepartment().getId().equals(dept.getId()))
                        .toList();

                if (deptNurses.isEmpty()) continue;

                // DAY → EVENING 인수인계
                if (shifts.size() >= 2) {
                    User nurse = deptNurses.get(random.nextInt(deptNurses.size()));
                    handovers.add(Handover.builder()
                            .department(dept)
                            .fromShift(shifts.get(0)) // DAY
                            .toShift(shifts.get(1))   // EVENING
                            .handoverDate(handoverDate)
                            .aiSummary(summaryTemplates[random.nextInt(summaryTemplates.length)])
                            .createdBy(nurse)
                            .build());
                }

                // EVENING → NIGHT 인수인계
                if (shifts.size() >= 3) {
                    User nurse = deptNurses.get(random.nextInt(deptNurses.size()));
                    handovers.add(Handover.builder()
                            .department(dept)
                            .fromShift(shifts.get(1)) // EVENING
                            .toShift(shifts.get(2))   // NIGHT
                            .handoverDate(handoverDate)
                            .aiSummary(summaryTemplates[random.nextInt(summaryTemplates.length)])
                            .createdBy(nurse)
                            .build());
                }
            }
        }

        return handoverRepository.saveAll(handovers);
    }
}
