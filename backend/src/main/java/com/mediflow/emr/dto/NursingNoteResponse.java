package com.mediflow.emr.dto;

import com.mediflow.emr.entity.enums.NoteCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 간호기록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NursingNoteResponse {

    private Long id;
    private Long patientId;
    private String patientName;
    private Long nurseId;
    private String nurseName;
    private String content;
    private String plainText;
    private NoteCategory category;
    private Boolean isImportant;
    private Boolean aiSuggested;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /** 현재 사용자가 수정 가능한지 여부 */
    private Boolean canEdit;
}
