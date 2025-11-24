package com.mediflow.emr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 온보딩 엔티티
 * 신규 간호사의 시스템 온보딩 진행 상태
 */
@Getter
@Entity
@Table(name = "onboarding")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Onboarding extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "onboarding_id")
    private Long id;

    /** 간호사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_id", nullable = false)
    private User nurse;

    /** 온보딩 단계 */
    @Column(nullable = false)
    private Integer step;

    /** 완료 여부 */
    @Column(nullable = false, name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = false;

    /** 완료 시간 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
