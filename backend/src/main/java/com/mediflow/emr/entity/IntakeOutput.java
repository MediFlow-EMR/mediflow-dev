package com.mediflow.emr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 섭취배설량 엔티티
 * 환자의 수분 섭취량 및 배설량 기록
 */
@Getter
@Entity
@Table(name = "intake_output")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntakeOutput extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "intake_output_id")
    private Long id;

    /** 환자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** 기록 간호사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_id", nullable = false)
    private User nurse;

    /** 경구 섭취량 (mL) */
    @Column(name = "intake_oral")
    private Integer intakeOral;

    /** 정맥 수액량 (mL) */
    @Column(name = "intake_iv")
    private Integer intakeIv;

    /** 소변량 (mL) */
    @Column(name = "output_urine")
    private Integer outputUrine;

    /** 배액량 (mL) */
    @Column(name = "output_drain")
    private Integer outputDrain;

    /** 기록 시간 */
    @Column(nullable = false, name = "recorded_at")
    private LocalDateTime recordedAt;
}
