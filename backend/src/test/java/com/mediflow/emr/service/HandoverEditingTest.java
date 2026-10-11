package com.mediflow.emr.service;

import com.mediflow.emr.config.GeminiConfig;
import com.mediflow.emr.dto.HandoverSaveRequest;
import com.mediflow.emr.entity.*;
import com.mediflow.emr.entity.enums.ShiftType;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandoverEditingTest {
    @Mock PatientRepository patients;
    @Mock NursingNoteRepository notes;
    @Mock VitalSignRepository vitals;
    @Mock MedicationRepository medications;
    @Mock IntakeOutputRepository intakeOutputs;
    @Mock TestResultRepository testResults;
    @Mock DepartmentRepository departments;
    @Mock ShiftRepository shifts;
    @Mock GeminiService gemini;
    @Mock HandoverRepository handovers;
    @Mock HandoverRevisionRepository revisions;
    @Mock AssignmentRepository assignments;
    @Mock GeminiConfig config;
    @Mock ClinicalAuditService audit;
    @InjectMocks HandoverService service;
    DepartmentEntity department = DepartmentEntity.builder().id(1L).name("시연부서").build();
    User author = User.builder().id(10L).name("작성자").department(department).build();
    User colleague = User.builder().id(20L).name("동료").department(department).build();
    Shift from = Shift.builder().id(1L).date(java.time.LocalDate.of(2026,10,9)).type(ShiftType.DAY).build();
    Shift to = Shift.builder().id(2L).date(java.time.LocalDate.of(2026,10,9)).type(ShiftType.EVENING).build();

    Handover record(String status) {
        Handover result = Handover.builder().id(100L).department(department).fromShift(from).toShift(to)
                .createdBy(author).status(status).sourceType("MANUAL").finalSummary("이전 내용").build();
        ReflectionTestUtils.setField(result, "version", 0L);
        when(handovers.findById(100L)).thenReturn(Optional.of(result));
        return result;
    }
    HandoverSaveRequest request(String status, Long version) {
        return new HandoverSaveRequest(1L, 1L, 2L, null, "수정 내용", "MANUAL", status, version);
    }
    void persist() {
        when(handovers.saveAndFlush(any())).thenAnswer(invocation -> {
            Handover record = invocation.getArgument(0);
            ReflectionTestUtils.setField(record, "id", 100L);
            ReflectionTestUtils.setField(record, "version", record.getVersion() == null ? 0L : record.getVersion() + 1);
            return record;
        });
    }
    @Test void manualDraftHasNoAiProvenanceAndSnapshotIsSaved() {
        when(departments.findById(1L)).thenReturn(Optional.of(department));
        when(shifts.findById(1L)).thenReturn(Optional.of(from));
        when(shifts.findById(2L)).thenReturn(Optional.of(to));
        persist();
        var saved = service.saveHandover(request("DRAFT", null), author);
        assertThat(saved.getStatus()).isEqualTo("DRAFT");
        assertThat(saved.getHandoverDate()).isEqualTo(java.time.LocalDate.of(2026,10,9));
        assertThat(saved.getAiSummary()).isNull();
        assertThat(saved.getAiModel()).isNull();
        assertThat(saved.getApprovedAt()).isNull();
        verify(revisions).save(argThat(revision -> "수정 내용".equals(revision.getFinalSummary())));
        verifyNoInteractions(gemini);
    }
    @Test void rejectsAiRecordWithoutOriginal() {
        var request = new HandoverSaveRequest(1L, 1L, 2L, "", "내용", "AI", "DRAFT", null);
        assertThatThrownBy(() -> service.saveHandover(request, author)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(handovers, revisions);
    }
    @Test void draftCanBeApprovedByAuthor() {
        record("DRAFT"); persist();
        var updated = service.updateHandover(100L, request("APPROVED", 0L), author);
        assertThat(updated.getStatus()).isEqualTo("APPROVED");
        assertThat(updated.getApprovedAt()).isNotNull();
        assertThat(updated.getVersion()).isEqualTo(1L);
    }
    @Test void approvedEditPreservesPreviousAndNewContent() {
        record("APPROVED"); persist();
        service.updateHandover(100L, request("APPROVED", 0L), author);
        ArgumentCaptor<HandoverRevision> snapshots = ArgumentCaptor.forClass(HandoverRevision.class);
        verify(revisions, times(2)).save(snapshots.capture());
        assertThat(snapshots.getAllValues()).extracting(HandoverRevision::getFinalSummary).containsExactly("이전 내용", "수정 내용");
    }
    @Test void humanEditingAiDraftKeepsAiOriginalAndModel() {
        var record = record("DRAFT");
        record.updateContent("AI 원본", "AI 원본", "AI", "DRAFT", "original-model");
        when(revisions.existsByHandoverId(100L)).thenReturn(true);
        persist();
        var updated = service.updateHandover(100L,
                new HandoverSaveRequest(1L, 1L, 2L, "AI 원본", "사람이 수정한 내용", "AI", "DRAFT", 0L), author);
        assertThat(updated.getAiSummary()).isEqualTo("AI 원본");
        assertThat(updated.getAiModel()).isEqualTo("original-model");
        assertThat(updated.getFinalSummary()).isEqualTo("사람이 수정한 내용");
        verify(revisions).save(argThat(revision -> "AI 원본".equals(revision.getAiSummary()) && "사람이 수정한 내용".equals(revision.getFinalSummary())));
    }
    @Test void colleagueCannotEditEvenWithinDepartment() {
        record("APPROVED");
        assertThatThrownBy(() -> service.updateHandover(100L, request("APPROVED", 0L), colleague))
                .isInstanceOfSatisfying(BusinessException.class, error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(revisions);
    }
    @Test void staleVersionCannotOverwrite() {
        record("DRAFT");
        assertThatThrownBy(() -> service.updateHandover(100L, request("DRAFT", 8L), author))
                .isInstanceOfSatisfying(BusinessException.class, error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        verifyNoInteractions(revisions);
    }
    @Test void approvedCannotBeDowngradedOrChangeAiOriginal() {
        var record = record("APPROVED");
        assertThatThrownBy(() -> service.updateHandover(100L, request("DRAFT", 0L), author)).isInstanceOf(BusinessException.class);
        record.updateContent("original", "이전 내용", "AI", "APPROVED", "test-model");
        assertThatThrownBy(() -> service.updateHandover(100L,
                new HandoverSaveRequest(1L, 1L, 2L, "changed original", "수정", "AI", "APPROVED", 0L), author)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(revisions);
    }
    @Test void listHidesOtherAuthorsDraftsAndDepartmentAccessIsChecked() {
        Handover draft = Handover.builder().id(101L).department(department).fromShift(from).toShift(to)
                .createdBy(author).status("DRAFT").build();
        Handover approved = Handover.builder().id(102L).department(department).fromShift(from).toShift(to)
                .createdBy(author).status("APPROVED").build();
        when(handovers.findByDepartmentIdOrderByHandoverDateDesc(1L)).thenReturn(List.of(draft, approved));
        assertThat(service.getHandoversByDepartment(1L, colleague)).extracting(dto -> dto.getId()).containsExactly(102L);
        assertThat(service.getHandoversByDepartment(1L, author)).hasSize(2);
        assertThatThrownBy(() -> service.getHandoversByDepartment(2L, author)).isInstanceOf(BusinessException.class);
    }
    @Test void draftHistoryIsPrivateButApprovedHistoryIsReadable() {
        var record = record("DRAFT");
        assertThatThrownBy(() -> service.getRevisions(100L, colleague)).isInstanceOf(BusinessException.class);
        record.updateContent(null, "내용", "MANUAL", "APPROVED", null);
        when(revisions.findByHandoverIdOrderByIdDesc(100L)).thenReturn(List.of());
        assertThat(service.getRevisions(100L, colleague)).isEmpty();
    }
}
