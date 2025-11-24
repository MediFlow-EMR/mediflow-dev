package com.mediflow.emr.service;

import com.mediflow.emr.dto.NursingNoteRequest;
import com.mediflow.emr.dto.NursingNoteResponse;
import com.mediflow.emr.entity.NursingNote;
import com.mediflow.emr.entity.Patient;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.NursingNoteRepository;
import com.mediflow.emr.repository.PatientRepository;
import com.mediflow.emr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 간호기록 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NursingNoteService {

    private final NursingNoteRepository nursingNoteRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    /**
     * 간호기록 등록
     */
    @Transactional
    public NursingNoteResponse createNursingNote(Long nurseId, NursingNoteRequest request) {
        log.info("간호기록 등록 - nurseId: {}, patientId: {}", nurseId, request.patientId());

        // 간호사 조회
        User nurse = userRepository.findById(nurseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 환자 조회
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PATIENT_NOT_FOUND));

        // 간호기록 생성
        NursingNote note = NursingNote.builder()
                .patient(patient)
                .nurse(nurse)
                .content(request.content())
                .plainText(request.plainText())
                .category(request.category())
                .isImportant(request.isImportant() != null ? request.isImportant() : false)
                .aiSuggested(false)
                .build();

        NursingNote saved = nursingNoteRepository.save(note);
        log.info("간호기록 등록 완료 - id: {}", saved.getId());

        return toResponse(saved, nurseId);
    }

    /**
     * 환자의 간호기록 목록 조회 (최신순)
     */
    public List<NursingNoteResponse> getPatientNursingNotes(Long patientId, Long currentUserId) {
        log.info("환자 간호기록 목록 조회 - patientId: {}, currentUserId: {}", patientId, currentUserId);

        // 환자 존재 확인
        if (!patientRepository.existsById(patientId)) {
            throw new BusinessException(ErrorCode.PATIENT_NOT_FOUND);
        }

        List<NursingNote> notes = nursingNoteRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        log.info("조회된 간호기록 수: {}", notes.size());

        return notes.stream()
                .map(note -> toResponse(note, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 간호기록 수정
     */
    @Transactional
    public NursingNoteResponse updateNursingNote(Long noteId, Long nurseId, NursingNoteRequest request) {
        log.info("간호기록 수정 - noteId: {}, nurseId: {}", noteId, nurseId);

        // 기록 조회
        NursingNote note = nursingNoteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

        // 권한 확인 (본인이 작성한 기록만 수정 가능)
        if (!note.getNurse().getId().equals(nurseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 작성한 기록만 수정할 수 있습니다");
        }

        // 기록 수정
        note.update(
                request.content(),
                request.plainText(),
                request.category(),
                request.isImportant() != null ? request.isImportant() : false
        );

        log.info("간호기록 수정 완료 - id: {}", note.getId());
        return toResponse(note, nurseId);
    }

    /**
     * 간호기록 삭제
     */
    @Transactional
    public void deleteNursingNote(Long noteId, Long nurseId) {
        log.info("간호기록 삭제 - noteId: {}, nurseId: {}", noteId, nurseId);

        // 기록 조회
        NursingNote note = nursingNoteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

        // 권한 확인 (본인이 작성한 기록만 삭제 가능)
        if (!note.getNurse().getId().equals(nurseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 작성한 기록만 삭제할 수 있습니다");
        }

        nursingNoteRepository.delete(note);
        log.info("간호기록 삭제 완료 - id: {}", noteId);
    }

    /**
     * Entity -> Response DTO 변환
     */
    private NursingNoteResponse toResponse(NursingNote note, Long currentUserId) {
        boolean canEdit = currentUserId != null && 
                         note.getNurse().getId().equals(currentUserId);
        
        return NursingNoteResponse.builder()
                .id(note.getId())
                .patientId(note.getPatient().getId())
                .patientName(note.getPatient().getName())
                .nurseId(note.getNurse().getId())
                .nurseName(note.getNurse().getName())
                .content(note.getContent())
                .plainText(note.getPlainText())
                .category(note.getCategory())
                .isImportant(note.getIsImportant())
                .aiSuggested(note.getAiSuggested())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .canEdit(canEdit)
                .build();
    }
}
