package com.mediflow.emr.service;

import com.mediflow.emr.entity.ClinicalAuditLog;
import com.mediflow.emr.repository.ClinicalAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClinicalAuditService {

    private final ClinicalAuditLogRepository repository;

    /** 감사 기록은 원 업무 트랜잭션과 분리해 append-only로 저장한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, Long patientId, String action,
                       String resourceType, Long resourceId, String detail) {
        repository.save(ClinicalAuditLog.builder()
                .userId(userId)
                .patientId(patientId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .detail(detail)
                .build());
    }
}
