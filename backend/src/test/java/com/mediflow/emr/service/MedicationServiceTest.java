package com.mediflow.emr.service;

import com.mediflow.emr.dto.MedicationRequest;
import com.mediflow.emr.entity.MedicalOrder;
import com.mediflow.emr.entity.Medication;
import com.mediflow.emr.entity.Patient;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.entity.enums.MedicationRoute;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.repository.MedicalOrderRepository;
import com.mediflow.emr.repository.MedicationRepository;
import com.mediflow.emr.repository.PatientRepository;
import com.mediflow.emr.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MedicationServiceTest {

    private final MedicationRepository medicationRepository = mock(MedicationRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PatientAccessService patientAccessService = mock(PatientAccessService.class);
    private final MedicalOrderRepository orderRepository = mock(MedicalOrderRepository.class);
    private final ClinicalAuditService auditService = mock(ClinicalAuditService.class);
    private final MedicationService service = new MedicationService(
            medicationRepository, patientRepository, userRepository, patientAccessService,
            orderRepository, auditService);

    @Test
    void administrationUsesCanonicalOrderDataAndCompletesOrderAtomically() {
        Patient patient = Patient.builder().id(100L).build();
        User nurse = User.builder().id(10L).name("간호사A").build();
        MedicalOrder order = medicationOrder(200L, patient, "IN_PROGRESS");
        when(userRepository.findById(10L)).thenReturn(Optional.of(nurse));
        when(patientAccessService.requireAccessiblePatient(10L, 100L)).thenReturn(patient);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(medicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createMedication(10L, request(200L, 100L));

        ArgumentCaptor<Medication> captor = ArgumentCaptor.forClass(Medication.class);
        verify(medicationRepository).save(captor.capture());
        Medication saved = captor.getValue();
        assertThat(saved.getMedicalOrder()).isSameAs(order);
        assertThat(saved.getDrugName()).isEqualTo("원처방약");
        assertThat(saved.getDose()).isEqualTo("5mg");
        assertThat(saved.getRoute()).isEqualTo(MedicationRoute.IV);
        assertThat(saved.getAdministrationNotes()).isEqualTo("이상 반응 없음");
        assertThat(order.getStatus()).isEqualTo("COMPLETED");
        assertThat(order.getCompletedBy()).isEqualTo("간호사A");
        verify(auditService).record(10L, 100L, "ADMINISTER", "MEDICATION", null, "orderId=200");
    }

    @Test
    void administrationRejectsOrderForAnotherPatient() {
        Patient selectedPatient = Patient.builder().id(100L).build();
        Patient otherPatient = Patient.builder().id(101L).build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(User.builder().id(10L).build()));
        when(patientAccessService.requireAccessiblePatient(10L, 100L)).thenReturn(selectedPatient);
        when(orderRepository.findById(200L))
                .thenReturn(Optional.of(medicationOrder(200L, otherPatient, "IN_PROGRESS")));

        assertThatThrownBy(() -> service.createMedication(10L, request(200L, 100L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("환자가 일치하지 않습니다");
        verify(medicationRepository, never()).save(any());
    }

    @Test
    void administrationRejectsAlreadyCompletedOrder() {
        Patient patient = Patient.builder().id(100L).build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(User.builder().id(10L).build()));
        when(patientAccessService.requireAccessiblePatient(10L, 100L)).thenReturn(patient);
        when(orderRepository.findById(200L))
                .thenReturn(Optional.of(medicationOrder(200L, patient, "COMPLETED")));

        assertThatThrownBy(() -> service.createMedication(10L, request(200L, 100L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 완료되었거나 취소된 처방");
        verify(medicationRepository, never()).save(any());
    }

    private MedicalOrder medicationOrder(Long id, Patient patient, String status) {
        return MedicalOrder.builder()
                .id(id)
                .patient(patient)
                .orderType("MEDICATION")
                .orderName("원처방약")
                .orderCode("DRUG-001")
                .dose("5mg")
                .route("IV")
                .frequency("BID")
                .status(status)
                .orderedAt(LocalDateTime.now().minusHours(1))
                .orderDoctor("의사A")
                .build();
    }

    private MedicationRequest request(Long orderId, Long patientId) {
        return MedicationRequest.builder()
                .orderId(orderId)
                .patientId(patientId)
                .drugName("클라이언트변조약")
                .dose("999mg")
                .route(MedicationRoute.PO)
                .administeredAt(LocalDateTime.now().minusMinutes(5))
                .notes("이상 반응 없음")
                .build();
    }
}
