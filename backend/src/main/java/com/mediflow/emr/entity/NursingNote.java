package com.mediflow.emr.entity;

import com.mediflow.emr.entity.enums.NoteCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 간호기록 엔티티
 * 환자에 대한 간호 관찰 및 처치 기록
 */
@Getter
@Entity
@Table(name = "nursing_note")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NursingNote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nursing_note_id")
    private Long id;

    /** 환자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** 작성 간호사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_id", nullable = false)
    private User nurse;

    /** 기록 내용 (Quill HTML) */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 기록 내용 (Plain Text) */
    @Column(columnDefinition = "TEXT", name = "plain_text")
    private String plainText;

    /** 기록 카테고리 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoteCategory category;

    /** 중요 표시 여부 (형광펜/볼드 강조) */
    @Column(nullable = false, name = "is_important")
    @Builder.Default
    private Boolean isImportant = false;

    /** AI 제안 여부 */
    @Column(nullable = false, name = "ai_suggested")
    @Builder.Default
    private Boolean aiSuggested = false;

    /**
     * 간호기록 수정
     */
    public void update(String content, String plainText, NoteCategory category, Boolean isImportant) {
        this.content = content;
        this.plainText = plainText;
        this.category = category;
        this.isImportant = isImportant;
    }
}
