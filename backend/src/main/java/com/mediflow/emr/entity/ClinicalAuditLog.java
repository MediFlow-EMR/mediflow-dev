package com.mediflow.emr.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 환자정보 접근과 임상기록 변경을 추적하기 위한 append-only 감사 로그. */
@Getter
@Entity
@Table(name = "clinical_audit_log", indexes = {
        @Index(name = "idx_audit_user_time", columnList = "user_id, occurred_at"),
        @Index(name = "idx_audit_patient_time", columnList = "patient_id, occurred_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClinicalAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(length = 500)
    private String detail;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    void stampTime() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
