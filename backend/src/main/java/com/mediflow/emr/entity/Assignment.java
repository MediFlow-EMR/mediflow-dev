package com.mediflow.emr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 환자 배정 엔티티
 * 특정 근무조에 간호사-환자 배정 정보
 */
@Getter
@Entity
@Table(name = "assignment")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long id;

    /** 배정된 간호사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_id", nullable = false)
    private User nurse;

    /** 배정된 환자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** 근무조 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    /** 배정 날짜 */
    @Column(nullable = false, name = "assigned_date")
    private LocalDate assignedDate;

    /** 주담당 여부 */
    @Column(nullable = false, name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;
}
