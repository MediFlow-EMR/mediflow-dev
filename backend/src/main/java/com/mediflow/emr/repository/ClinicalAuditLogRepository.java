package com.mediflow.emr.repository;

import com.mediflow.emr.entity.ClinicalAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClinicalAuditLogRepository extends JpaRepository<ClinicalAuditLog, Long> {
    List<ClinicalAuditLog> findByPatientIdOrderByOccurredAtDesc(Long patientId);
    List<ClinicalAuditLog> findByUserIdOrderByOccurredAtDesc(Long userId);
}
