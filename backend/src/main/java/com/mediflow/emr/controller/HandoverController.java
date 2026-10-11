package com.mediflow.emr.controller;

import com.mediflow.emr.dto.ApiResponse;
import com.mediflow.emr.dto.HandoverDto;
import com.mediflow.emr.dto.HandoverSaveRequest;
import com.mediflow.emr.entity.Handover;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.UserRepository;
import com.mediflow.emr.service.HandoverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/handovers")
@RequiredArgsConstructor
public class HandoverController {

    private final HandoverService handoverService;
    private final UserRepository userRepository;

    @PostMapping("/ai-summary")
    public ResponseEntity<ApiResponse<String>> generateAiSummary(
            Authentication authentication,
            @RequestParam Long departmentId,
            @RequestParam Long fromShiftId) {
        
        String providerId = authentication.getName();
        log.info("AI 인수인계 요약 요청 - providerId: {}, departmentId: {}, fromShiftId: {}", 
                providerId, departmentId, fromShiftId);
        
        // 사용자 확인
        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        String aiSummary = handoverService.generateAiSummary(user.getId(), fromShiftId);
        
        return ResponseEntity.ok(ApiResponse.ok(aiSummary));
    }

    @PostMapping("/ai-summary/patient/{patientId}")
    public ResponseEntity<ApiResponse<String>> regeneratePatientAiSummary(
            Authentication authentication,
            @PathVariable Long patientId,
            @RequestParam Long fromShiftId) {
        User user = userRepository.findByProviderId(authentication.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String aiSummary = handoverService.generatePatientAiSummary(user.getId(), fromShiftId, patientId);
        return ResponseEntity.ok(ApiResponse.ok(aiSummary));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByProviderId(authentication.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HandoverDto>> saveHandover(Authentication authentication,
            @Valid @RequestBody HandoverSaveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(handoverService.saveHandover(request, currentUser(authentication))));
    }

    @PutMapping("/{handoverId}")
    public ResponseEntity<ApiResponse<HandoverDto>> updateHandover(Authentication authentication,
            @PathVariable Long handoverId, @Valid @RequestBody HandoverSaveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(handoverService.updateHandover(handoverId, request, currentUser(authentication))));
    }

    @GetMapping("/{handoverId}/revisions")
    public ResponseEntity<ApiResponse<List<com.mediflow.emr.entity.HandoverRevision>>> getRevisions(
            Authentication authentication, @PathVariable Long handoverId) {
        return ResponseEntity.ok(ApiResponse.ok(handoverService.getRevisions(handoverId, currentUser(authentication))));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<ApiResponse<List<HandoverDto>>> getHandoversByDepartment(
            Authentication authentication, @PathVariable Long departmentId) {
        return ResponseEntity.ok(ApiResponse.ok(handoverService.getHandoversByDepartment(departmentId, currentUser(authentication))));
    }

    @DeleteMapping("/{handoverId}")
    public ResponseEntity<ApiResponse<Void>> deleteHandover(
            Authentication authentication,
            @PathVariable Long handoverId) {
        
        String providerId = authentication.getName();
        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        handoverService.deleteHandover(handoverId, user.getId());
        
        return ResponseEntity.ok(ApiResponse.ok(null, "인수인계가 삭제되었습니다"));
    }
}
