package com.mediflow.emr.controller;

import com.mediflow.emr.dto.ApiResponse;
import com.mediflow.emr.dto.DepartmentSummaryResponse;
import com.mediflow.emr.dto.MyPatientResponse;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.UserRepository;
import com.mediflow.emr.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;
import com.mediflow.emr.entity.enums.ShiftType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 대시보드 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    /**
     * 내 담당 환자 목록 조회
     *
     * API: GET /api/dashboard/my-patients
     *
     * 설명:
     * - 현재 로그인한 간호사에게 배정된 환자만 조회
     * - 현재 근무조(주간/초번/야간) 기준
     * - 선택 날짜·근무조 기준 (생략하면 현재 날짜·근무조)
     *
     * 응답:
     * - 환자 기본 정보 (이름, 나이, 성별, 진단명 등)
     * - 최근 바이탈 사인 (혈압, 심박수, 체온 등)
     * - 주담당 여부 (isPrimary)
     */
    @GetMapping("/my-patients")
    public ResponseEntity<ApiResponse<List<MyPatientResponse>>> getMyPatients(Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ShiftType shiftType) {
        
        String providerId = authentication.getName(); // JWT subject (providerId)
        log.info("내 담당 환자 목록 조회 요청: providerId={}", providerId);
        
        // providerId로 사용자 조회
        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        List<MyPatientResponse> patients = dashboardService.getMyPatients(user.getId(), date, shiftType);
        
        return ResponseEntity.ok(ApiResponse.ok(patients));
    }

    /**
     * 전체 환자 목록 조회
     *
     * API: GET /api/dashboard/all-patients
     *
     * 설명:
     * - 간호사가 속한 부서의 입원 중인 모든 환자 조회
     * - 내 담당이 아닌 환자도 포함
     * - 간호사가 같은 부서의 다른 간호사의 환자도 볼 수 있도록 함
     *
     * 용도:
     * - "전체 환자" 탭 클릭 시 사용
     * - 부서 전체 상황 파악
     * - 다른 간호사 환자 상태 확인 (협업)
     *
     * 응답:
     * - 환자 기본 정보 + 최근 바이탈 사인
     * - isPrimary: 현재 로그인한 간호사의 담당 여부 표시
     *   (true: 내 담당, false: 다른 간호사 담당)
     *
     * 성능 최적화:
     * - N+1 쿼리 문제 해결 (배치 조회)
     * - 상세 내용은 DashboardService.getAllPatients() 참고
     */
    @GetMapping("/all-patients")
    public ResponseEntity<ApiResponse<List<MyPatientResponse>>> getAllPatients(Authentication authentication) {

        String providerId = authentication.getName(); // JWT subject (providerId)
        log.info("전체 환자 목록 조회 요청: providerId={}", providerId);

        // providerId로 사용자 조회
        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<MyPatientResponse> patients = dashboardService.getAllPatients(user.getId());

        return ResponseEntity.ok(ApiResponse.ok(patients));
    }

    /**
     * 부서 요약 정보 조회
     */
    @GetMapping("/department-summary")
    public ResponseEntity<ApiResponse<DepartmentSummaryResponse>> getDepartmentSummary(Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ShiftType shiftType) {
        
        String providerId = authentication.getName(); // JWT subject (providerId)
        log.info("부서 요약 정보 조회 요청: providerId={}", providerId);
        
        // providerId로 사용자 조회
        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        DepartmentSummaryResponse summary = dashboardService.getDepartmentSummary(user.getId(), date, shiftType);
        
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
