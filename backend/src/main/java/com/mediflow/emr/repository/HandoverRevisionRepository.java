package com.mediflow.emr.repository;
import com.mediflow.emr.entity.HandoverRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HandoverRevisionRepository extends JpaRepository<HandoverRevision, Long> {
    List<HandoverRevision> findByHandoverIdOrderByIdDesc(Long handoverId);
    boolean existsByHandoverId(Long handoverId);
}
