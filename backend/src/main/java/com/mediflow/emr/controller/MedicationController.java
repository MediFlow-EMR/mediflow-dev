package com.mediflow.emr.controller;

import com.mediflow.emr.dto.ApiResponse;
import com.mediflow.emr.dto.DrugSearchResult;
import com.mediflow.emr.dto.DrugDetailInfo;
import com.mediflow.emr.dto.MedicationRequest;
import com.mediflow.emr.dto.MedicationResponse;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.UserRepository;
import com.mediflow.emr.service.DrugApiService;
import com.mediflow.emr.service.MedicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 투약 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/medications")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService medicationService;
    private final UserRepository userRepository;
    private final DrugApiService drugApiService;

    /**
     * 투약 기록 등록
     */
    @PostMapping
    public ApiResponse<MedicationResponse> createMedication(
            Authentication authentication,
            @Valid @RequestBody MedicationRequest request
    ) {
        String providerId = authentication.getName();
        log.info("투약 등록 요청 - providerId: {}, patientId: {}", 
                providerId, request.patientId());

        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        MedicationResponse response = medicationService.createMedication(nurse.getId(), request);
        return ApiResponse.ok(response, "투약 기록이 등록되었습니다");
    }

    /**
     * 환자의 투약 기록 목록 조회
     */
    @GetMapping("/patient/{patientId}")
    public ApiResponse<List<MedicationResponse>> getPatientMedications(
            Authentication authentication,
            @PathVariable Long patientId
    ) {
        String providerId = authentication.getName();
        log.info("환자 투약 목록 조회 - providerId: {}, patientId: {}", providerId, patientId);
        
        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        List<MedicationResponse> medications = medicationService.getPatientMedications(patientId, nurse.getId());
        return ApiResponse.ok(medications);
    }

    /**
     * 투약 기록 수정
     */
    @PutMapping("/{medicationId}")
    public ApiResponse<MedicationResponse> updateMedication(
            Authentication authentication,
            @PathVariable Long medicationId,
            @Valid @RequestBody MedicationRequest request
    ) {
        String providerId = authentication.getName();
        log.info("투약 수정 요청 - providerId: {}, medicationId: {}", providerId, medicationId);

        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        MedicationResponse response = medicationService.updateMedication(medicationId, nurse.getId(), request);
        return ApiResponse.ok(response, "투약 기록이 수정되었습니다");
    }

    /**
     * 투약 기록 삭제
     */
    @DeleteMapping("/{medicationId}")
    public ApiResponse<Void> deleteMedication(
            Authentication authentication,
            @PathVariable Long medicationId
    ) {
        String providerId = authentication.getName();
        log.info("투약 삭제 요청 - providerId: {}, medicationId: {}", providerId, medicationId);

        // providerId로 사용자 조회
        User nurse = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        medicationService.deleteMedication(medicationId, nurse.getId());
        return ApiResponse.ok(null, "투약 기록이 삭제되었습니다");
    }

    /**
     * 약품 검색 (식약처 API 연동)
     */
    @GetMapping("/drugs/search")
    public ApiResponse<List<DrugSearchResult>> searchDrugs(
            @RequestParam String keyword
    ) {
        log.info("약품 검색 - keyword: {}", keyword);
        
        if (keyword == null || keyword.trim().length() < 2) {
            return ApiResponse.ok(List.of());
        }
        
        List<DrugSearchResult> results = drugApiService.searchDrugs(keyword.trim());
        return ApiResponse.ok(results);
    }

    /** 품목번호로 허가정보 조회. 처방/환자 데이터를 변경하지 않는 약품 카탈로그 API. */
    @GetMapping("/drugs/{itemSeq}/detail")
    public ApiResponse<DrugDetailInfo> getDrugDetailByItemSeq(@PathVariable String itemSeq) {
        if (!itemSeq.matches("[0-9]{9}")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return ApiResponse.ok(drugApiService.getDrugDetailByItemSeq(itemSeq));
    }
}
