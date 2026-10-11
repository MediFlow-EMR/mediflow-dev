package com.mediflow.emr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    /** 수정 전 AI 원본 초안 */
    @Column(columnDefinition = "TEXT", name = "ai_summary")
    private String aiSummary;

    /** 의료인이 확인·수정한 최종 인수인계 */
    @Column(columnDefinition = "TEXT", name = "final_summary")
    private String finalSummary;

    /** 생성에 사용한 모델과 최종 승인 상태 */
    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "APPROVED";

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Builder.Default
    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType = "AI";

    public void updateContent(String aiDraft, String content, String source, String nextStatus, String model) {
        this.aiSummary = aiDraft;
        this.finalSummary = content;
        this.sourceType = source;
        this.aiModel = model;
        this.status = nextStatus;
        if ("APPROVED".equals(nextStatus)) this.approvedAt = LocalDateTime.now();
    }

    /** 작성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}
