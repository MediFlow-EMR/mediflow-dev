package com.mediflow.emr.controller;

import com.mediflow.emr.dto.ApiResponse;
import com.mediflow.emr.dto.IntakeOutputRequest;
import com.mediflow.emr.dto.IntakeOutputResponse;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.UserRepository;
import com.mediflow.emr.service.IntakeOutputService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 섭취배설량 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/intake-output")
@RequiredArgsConstructor
public class IntakeOutputController {

    private final IntakeOutputService intakeOutputService;
    private final UserRepository userRepository;

    /**
     * 섭취배설량 등록
     */
    @PostMapping
    public ApiResponse<IntakeOutputResponse> createIntakeOutput(
            Authentication authentication,
            @Valid @RequestBody IntakeOutputRequest request
    ) {
        String providerId = authentication.getName();
        log.info("I/O 등록 요청 - providerId: {}, patientId: {}", 
                providerId, request.patientId());

        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        IntakeOutputResponse response = intakeOutputService.createIntakeOutput(nurse.getId(), request);
        return ApiResponse.ok(response, "섭취배설량이 등록되었습니다");
    }

    /**
     * 환자의 섭취배설량 목록 조회
     */
    @GetMapping("/patient/{patientId}")
    public ApiResponse<List<IntakeOutputResponse>> getPatientIntakeOutputs(
            Authentication authentication,
            @PathVariable Long patientId
    ) {
        String providerId = authentication.getName();
        log.info("환자 I/O 목록 조회 - providerId: {}, patientId: {}", providerId, patientId);
        
        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        List<IntakeOutputResponse> records = intakeOutputService.getPatientIntakeOutputs(patientId, nurse.getId());
        return ApiResponse.ok(records);
    }

    /**
     * 섭취배설량 수정
     */
    @PutMapping("/{recordId}")
    public ApiResponse<IntakeOutputResponse> updateIntakeOutput(
            Authentication authentication,
            @PathVariable Long recordId,
            @Valid @RequestBody IntakeOutputRequest request
    ) {
        String providerId = authentication.getName();
        log.info("I/O 수정 요청 - providerId: {}, recordId: {}", providerId, recordId);

        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        IntakeOutputResponse response = intakeOutputService.updateIntakeOutput(recordId, nurse.getId(), request);
        return ApiResponse.ok(response, "섭취배설량이 수정되었습니다");
    }

    /**
     * 섭취배설량 삭제
     */
    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> deleteIntakeOutput(
            Authentication authentication,
            @PathVariable Long recordId
    ) {
        String providerId = authentication.getName();
        log.info("I/O 삭제 요청 - providerId: {}, recordId: {}", providerId, recordId);

        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        intakeOutputService.deleteIntakeOutput(recordId, nurse.getId());
        return ApiResponse.ok(null, "섭취배설량이 삭제되었습니다");
    }
}
