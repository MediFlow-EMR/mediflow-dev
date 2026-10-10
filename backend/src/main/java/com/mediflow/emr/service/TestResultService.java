package com.mediflow.emr.service;

import com.mediflow.emr.dto.TestResultResponse;
import com.mediflow.emr.entity.TestResult;
import com.mediflow.emr.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 검사 결과 서비스 (조회 전용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestResultService {

    private final TestResultRepository testResultRepository;
    private final PatientAccessService patientAccessService;

    /**
     * 환자별 검사 결과 목록 조회
     */
    public List<TestResultResponse> getTestResultsByPatient(Long patientId, Long currentUserId) {
        log.info("환자 검사 결과 조회 - patientId: {}", patientId);

        patientAccessService.requireAccessiblePatient(currentUserId, patientId);
        
        List<TestResult> testResults = testResultRepository.findByPatientIdOrderByResultDateDesc(patientId);
        
        return testResults.stream()
                .map(TestResultResponse::from)
                .collect(Collectors.toList());
    }
}
