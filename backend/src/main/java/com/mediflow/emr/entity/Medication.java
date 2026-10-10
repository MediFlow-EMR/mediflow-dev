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

    /** 이 투약 수행의 근거가 된 처방. 과거 시드 데이터와의 호환을 위해 nullable이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_order_id")
    private MedicalOrder medicalOrder;

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

    /** 투약 당시 관찰한 특이사항 */
    @Column(name = "administration_notes", length = 1000)
    private String administrationNotes;

    /**
     * 투약 기록 수정
     */
    public void update(String drugName, String drugCode, String dose, 
                      MedicationRoute route, String frequency, 
                      LocalDateTime administeredAt, String orderDoctor) {
        this.drugName = drugName;
        this.drugCode = drugCode;
        this.dose = dose;
        this.route = route;
        this.frequency = frequency;
        this.administeredAt = administeredAt;
        this.orderDoctor = orderDoctor;
    }
}
