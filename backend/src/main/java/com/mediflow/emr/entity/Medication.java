package com.mediflow.emr.entity;

import com.mediflow.emr.entity.enums.MedicationRoute;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 투약 엔티티
 * 환자의 약물 투여 기록
 */
@Getter
@Entity
@Table(name = "medication")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medication_id")
    private Long id;

    /** 환자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** 투약 간호사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_id", nullable = false)
    private User nurse;

    /** 약물명 */
    @Column(nullable = false, name = "drug_name", length = 200)
    private String drugName;

    /** 약물 코드 (식약처 코드) */
    @Column(name = "drug_code", length = 50)
    private String drugCode;

    /** 용량 */
    @Column(length = 100)
    private String dose;

    /** 투약 경로 (PO, IV, IM, SC) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MedicationRoute route;

    /** 투약 빈도 */
    @Column(length = 100)
    private String frequency;

    /** 투약 시간 */
    @Column(nullable = false, name = "administered_at")
    private LocalDateTime administeredAt;

    /** 처방 의사 */
    @Column(name = "order_doctor", length = 100)
    private String orderDoctor;
}
