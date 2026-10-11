package com.mediflow.emr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Append-only content snapshots, committed with the handover update. */
@Getter
@Entity
@Table(name = "handover_revision", indexes = @Index(name = "idx_handover_revision", columnList = "handover_id, revision_id"))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HandoverRevision {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "revision_id")
    private Long id;
    @Column(name = "handover_id", nullable = false, updatable = false)
    private Long handoverId;
    @Column(name = "edited_by_id", nullable = false, updatable = false)
    private Long editedById;
    @Column(name = "edited_by_name", nullable = false, updatable = false, length = 100)
    private String editedByName;
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;
    @Column(nullable = false, updatable = false, length = 20)
    private String status;
    @Column(name = "source_type", nullable = false, updatable = false, length = 20)
    private String sourceType;
    @Column(name = "ai_summary", columnDefinition = "TEXT", updatable = false)
    private String aiSummary;
    @Column(name = "final_summary", columnDefinition = "TEXT", updatable = false)
    private String finalSummary;
    @Column(name = "ai_model", updatable = false, length = 100)
    private String aiModel;
    @Column(name = "record_version", updatable = false)
    private Long recordVersion;
}
