package com.mediflow.emr.controller;

import com.mediflow.emr.dto.ApiResponse;
import com.mediflow.emr.dto.VitalSignRequest;
import com.mediflow.emr.dto.VitalSignResponse;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.UserRepository;
import com.mediflow.emr.service.VitalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 바이탈 사인 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/vitals")
@RequiredArgsConstructor
public class VitalController {

    private final VitalService vitalService;
    private final UserRepository userRepository;

    /**
     * 바이탈 사인 등록
     */
    @PostMapping
    public ApiResponse<VitalSignResponse> createVitalSign(
            Authentication authentication,
            @Valid @RequestBody VitalSignRequest request
    ) {
        String providerId = authentication.getName();
        log.info("바이탈 등록 요청 - providerId: {}, patientId: {}", 
                providerId, request.patientId());

        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        VitalSignResponse response = vitalService.createVitalSign(nurse.getId(), request);
        return ApiResponse.ok(response, "바이탈 사인이 등록되었습니다");
    }

    /**
     * 환자의 바이탈 사인 목록 조회
     */
    @GetMapping("/patient/{patientId}")
    public ApiResponse<List<VitalSignResponse>> getPatientVitals(
            Authentication authentication,
            @PathVariable Long patientId
    ) {
        String providerId = authentication.getName();
        log.info("환자 바이탈 목록 조회 - providerId: {}, patientId: {}", providerId, patientId);
        
        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        List<VitalSignResponse> vitals = vitalService.getPatientVitals(patientId, nurse.getId());
        return ApiResponse.ok(vitals);
    }

    /**
     * 환자의 최근 바이탈 사인 조회
     */
    @GetMapping("/patient/{patientId}/latest")
    public ApiResponse<VitalSignResponse> getLatestVital(
            Authentication authentication,
            @PathVariable Long patientId
    ) {
        log.info("최근 바이탈 조회 - patientId: {}", patientId);

        User user = userRepository.findByProviderId(authentication.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        VitalSignResponse vital = vitalService.getLatestVital(patientId, user.getId());
        return ApiResponse.ok(vital);
    }

    /**
     * 바이탈 사인 수정
     */
    @PutMapping("/{vitalId}")
    public ApiResponse<VitalSignResponse> updateVitalSign(
            Authentication authentication,
            @PathVariable Long vitalId,
            @Valid @RequestBody VitalSignRequest request
    ) {
        String providerId = authentication.getName();
        log.info("바이탈 수정 요청 - providerId: {}, vitalId: {}", providerId, vitalId);

        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        VitalSignResponse response = vitalService.updateVitalSign(vitalId, nurse.getId(), request);
        return ApiResponse.ok(response, "바이탈 사인이 수정되었습니다");
    }

    /**
     * 바이탈 사인 삭제
     */
    @DeleteMapping("/{vitalId}")
    public ApiResponse<Void> deleteVitalSign(
            Authentication authentication,
            @PathVariable Long vitalId
    ) {
        String providerId = authentication.getName();
        log.info("바이탈 삭제 요청 - providerId: {}, vitalId: {}", providerId, vitalId);

        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        vitalService.deleteVitalSign(vitalId, nurse.getId());
        return ApiResponse.ok(null, "바이탈 사인이 삭제되었습니다");
    }

}
