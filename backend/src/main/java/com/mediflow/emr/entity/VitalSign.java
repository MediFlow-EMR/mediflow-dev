package com.mediflow.emr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 바이탈 사인 엔티티
 * 환자의 활력징후 측정 기록
 */
@Getter
@Entity
@Table(name = "vital_sign")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalSign extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vital_sign_id")
    private Long id;

    /** 환자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** 측정 간호사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_id", nullable = false)
    private User nurse;

    /** 수축기 혈압 (mmHg) */
    @Column(name = "systolic_bp")
    private Integer systolicBp;

    /** 이완기 혈압 (mmHg) */
    @Column(name = "diastolic_bp")
    private Integer diastolicBp;

    /** 심박수 (회/분) */
    @Column(name = "heart_rate")
    private Integer heartRate;

    /** 체온 (°C) */
    @Column(name = "body_temp")
    private Double bodyTemp;

    /** 호흡수 (회/분) */
    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    /** 산소포화도 (%) */
    @Column(name = "spo2")
    private Integer spo2;

    /** 측정 시간 */
    @Column(nullable = false, name = "measured_at")
    private LocalDateTime measuredAt;
}
