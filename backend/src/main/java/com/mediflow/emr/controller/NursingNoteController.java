package com.mediflow.emr.controller;

import com.mediflow.emr.dto.ApiResponse;
import com.mediflow.emr.dto.NursingNoteRequest;
import com.mediflow.emr.dto.NursingNoteResponse;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.UserRepository;
import com.mediflow.emr.service.NursingNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 간호기록 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/nursing-notes")
@RequiredArgsConstructor
public class NursingNoteController {

    private final NursingNoteService nursingNoteService;
    private final UserRepository userRepository;

    /**
     * 간호기록 등록
     */
    @PostMapping
    public ApiResponse<NursingNoteResponse> createNursingNote(
            Authentication authentication,
            @Valid @RequestBody NursingNoteRequest request
    ) {
        String providerId = authentication.getName();
        log.info("간호기록 등록 요청 - providerId: {}, patientId: {}", 
                providerId, request.patientId());

        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        NursingNoteResponse response = nursingNoteService.createNursingNote(nurse.getId(), request);
        return ApiResponse.ok(response, "간호기록이 등록되었습니다");
    }

    /**
     * 환자의 간호기록 목록 조회
     */
    @GetMapping("/patient/{patientId}")
    public ApiResponse<List<NursingNoteResponse>> getPatientNursingNotes(
            Authentication authentication,
            @PathVariable Long patientId
    ) {
        String providerId = authentication.getName();
        log.info("환자 간호기록 목록 조회 - providerId: {}, patientId: {}", providerId, patientId);
        
        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        List<NursingNoteResponse> notes = nursingNoteService.getPatientNursingNotes(patientId, nurse.getId());
        return ApiResponse.ok(notes);
    }

    /**
     * 간호기록 수정
     */
    @PutMapping("/{noteId}")
    public ApiResponse<NursingNoteResponse> updateNursingNote(
            Authentication authentication,
            @PathVariable Long noteId,
            @Valid @RequestBody NursingNoteRequest request
    ) {
        String providerId = authentication.getName();
        log.info("간호기록 수정 요청 - providerId: {}, noteId: {}", providerId, noteId);

        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        NursingNoteResponse response = nursingNoteService.updateNursingNote(noteId, nurse.getId(), request);
        return ApiResponse.ok(response, "간호기록이 수정되었습니다");
    }

    /**
     * 간호기록 삭제
     */
    @DeleteMapping("/{noteId}")
    public ApiResponse<Void> deleteNursingNote(
            Authentication authentication,
            @PathVariable Long noteId
    ) {
        String providerId = authentication.getName();
        log.info("간호기록 삭제 요청 - providerId: {}, noteId: {}", providerId, noteId);

        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        nursingNoteService.deleteNursingNote(noteId, nurse.getId());
        return ApiResponse.ok(null, "간호기록이 삭제되었습니다");
    }
}
