package com.mediflow.emr.entity;

import com.mediflow.emr.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 환자 엔티티
 * 병원에 내원한 환자의 기본 정보
 */
@Getter
@Entity
@Table(name = "patient")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Long id;

    /** 차트번호 (병원 고유 환자 식별번호) */
    @Column(nullable = false, unique = true, length = 50)
    private String chartNumber;

    /** 환자명 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 나이 */
    @Column(nullable = false)
    private Integer age;

    /** 성별 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private Gender gender;

    /** 주민등록번호 앞 7자리 */
    @Column(length = 7)
    private String ssn;

    /** 주진단명 */
    @Column(length = 500)
    private String diagnosis;

    /** 입원일 */
    @Column(name = "admission_date")
    private LocalDate admissionDate;

    /** 병실 및 침상 — 입원환자 식별을 위한 현재 위치 */
    @Column(name = "room_number", length = 20)
    private String roomNumber;

    @Column(name = "bed_number", length = 20)
    private String bedNumber;

    /** 현재 주치의 표시명 */
    @Column(name = "attending_physician", length = 100)
    private String attendingPhysician;

    /** 병동 안전 표지 */
    @Column(name = "fall_risk", nullable = false)
    @Builder.Default
    private Boolean fallRisk = false;

    @Column(name = "isolation_type", length = 50)
    private String isolationType;

    /** 퇴원일 */
    @Column(name = "discharge_date")
    private LocalDate dischargeDate;

    /** 혈액형 */
    @Column(length = 10)
    private String bloodType;

    /** 알레르기 정보 */
    @Column(length = 500)
    private String allergies;

    /** 보호자명 */
    @Column(name = "guardian_name", length = 100)
    private String guardianName;

    /** 보호자 연락처 */
    @Column(name = "guardian_phone", length = 20)
    private String guardianPhone;

    /** 응급 중증도 (1-5, 응급실만 해당) */
    @Column(name = "triage_level")
    private Integer triageLevel;

    /** 입원 여부 */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isAdmitted = false;

    /** 현재 소속 부서 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentEntity department;
}
