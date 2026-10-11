package com.mediflow.emr.service;

import com.mediflow.emr.dto.HandoverSaveRequest;
import com.mediflow.emr.entity.*;
import com.mediflow.emr.entity.enums.*;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:handover-persistence;MODE=MariaDB;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class HandoverPersistenceTest {
    @Autowired HandoverService service;
    @Autowired DepartmentRepository departments;
    @Autowired UserRepository users;
    @Autowired ShiftRepository shifts;
    @Autowired HandoverRevisionRepository revisions;
    @Test void persistedDraftReopensApprovesAndKeepsAllVersionsAcrossTransactions() {
        var department = departments.save(DepartmentEntity.builder().name("가상부서").code("HTEST").type(Department.INTERNAL_MEDICINE).build());
        var user = users.save(User.builder().email("handover@test.invalid").nickname("작성자").name("작성자")
                .provider(Provider.LOCAL).providerId("handover-test").department(department).build());
        var from = shifts.save(Shift.builder().date(LocalDate.now()).type(ShiftType.DAY).startTime(LocalTime.of(8,0)).endTime(LocalTime.of(16,0)).build());
        var to = shifts.save(Shift.builder().date(LocalDate.now()).type(ShiftType.EVENING).startTime(LocalTime.of(16,0)).endTime(LocalTime.MIDNIGHT).build());
        var draft = service.saveHandover(new HandoverSaveRequest(department.getId(), from.getId(), to.getId(), null, "첫 초안", "MANUAL", "DRAFT", null), user);
        var reopened = service.getHandoversByDepartment(department.getId(), user).getFirst();
        assertThat(reopened.getFinalSummary()).isEqualTo("첫 초안");
        assertThat(reopened.getStatus()).isEqualTo("DRAFT");
        var approved = service.updateHandover(draft.getId(), new HandoverSaveRequest(department.getId(), from.getId(), to.getId(), null, "승인 내용", "MANUAL", "APPROVED", reopened.getVersion()), user);
        var modified = service.updateHandover(draft.getId(), new HandoverSaveRequest(department.getId(), from.getId(), to.getId(), null, "승인 후 수정", "MANUAL", "APPROVED", approved.getVersion()), user);
        assertThat(service.getHandoversByDepartment(department.getId(), user).getFirst().getFinalSummary()).isEqualTo("승인 후 수정");
        assertThat(revisions.findByHandoverIdOrderByIdDesc(draft.getId())).extracting(HandoverRevision::getFinalSummary)
                .containsExactly("승인 후 수정", "승인 내용", "첫 초안");
        assertThatThrownBy(() -> service.updateHandover(draft.getId(), new HandoverSaveRequest(department.getId(), from.getId(), to.getId(), null, "오래된 변경", "MANUAL", "APPROVED", reopened.getVersion()), user)).isInstanceOf(BusinessException.class);
        assertThat(service.getRevisions(draft.getId(), user)).hasSize(3);
        assertThat(modified.getVersion()).isGreaterThan(reopened.getVersion());
    }
}
