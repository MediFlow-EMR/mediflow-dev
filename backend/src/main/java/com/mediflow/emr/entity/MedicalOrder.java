package com.mediflow.emr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 의료 오더 엔티티
 */
@Entity
@Table(name = "medical_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false)
    private String orderType; // MEDICATION, LAB, IMAGING, PROCEDURE

    @Column(nullable = false)
    private String orderName;

    private String orderCode; // 약품코드, 검사코드 등

    private String dose; // 용량 (투약 오더인 경우)

    private String route; // 투약 경로 (투약 오더인 경우)

    private String frequency; // 투약 빈도 (투약 오더인 경우)

    @Column(columnDefinition = "TEXT")
    private String instructions; // 지시사항

    @Column(nullable = false)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    private String orderDoctor; // 처방 의사

    private LocalDateTime completedAt; // 완료 시간

    private String completedBy; // 완료자 (간호사)

    @Column(columnDefinition = "TEXT")
    private String notes; // 비고

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 오더 상태 업데이트
     */
    public void updateStatus(String status, String completedBy) {
        this.status = status;
        if ("COMPLETED".equals(status)) {
            this.completedAt = LocalDateTime.now();
            this.completedBy = completedBy;
        }
    }

    /**
     * 오더 정보 업데이트
     */
    public void update(String orderName, String dose, String route, String frequency, String instructions) {
        if (orderName != null) this.orderName = orderName;
        if (dose != null) this.dose = dose;
        if (route != null) this.route = route;
        if (frequency != null) this.frequency = frequency;
        if (instructions != null) this.instructions = instructions;
    }
}
