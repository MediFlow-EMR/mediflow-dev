package com.mediflow.emr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 인수인계 엔티티
 * 근무조 간 환자 인수인계 정보
 */
@Getter
@Entity
@Table(name = "handover")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Handover extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "handover_id")
    private Long id;

    /** 부서 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private DepartmentEntity department;

    /** 인계 근무조 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_shift_id", nullable = false)
    private Shift fromShift;

    /** 인수 근무조 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_shift_id", nullable = false)
    private Shift toShift;

    /** 인수인계 날짜 */
    @Column(nullable = false, name = "handover_date")
    private LocalDate handoverDate;

    /** AI 요약 내용 */
    @Column(columnDefinition = "TEXT", name = "ai_summary")
    private String aiSummary;

    /** 추가 특이사항 */
    @Column(columnDefinition = "TEXT", name = "additional_notes")
    private String additionalNotes;

    /** 작성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}
