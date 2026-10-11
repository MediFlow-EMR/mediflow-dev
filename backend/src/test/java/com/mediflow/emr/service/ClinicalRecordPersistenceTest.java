package com.mediflow.emr.service;

import com.mediflow.emr.dto.*;
import com.mediflow.emr.entity.*;
import com.mediflow.emr.entity.enums.*;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:handover-persistence;MODE=MariaDB;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ClinicalRecordPersistenceTest {
    @Autowired NursingNoteService notes;
    @Autowired VitalService vitals;
    @Autowired IntakeOutputService io;
    @Autowired DepartmentRepository departments;
    @Autowired UserRepository users;
    @Autowired PatientRepository patients;

    @Test void clinicalUpdatesReturnNewVersionsAndRejectStaleWritesAcrossTransactions() {
        var department = departments.save(DepartmentEntity.builder().name("기록검증부서").code("RECORDTEST").type(Department.INTERNAL_MEDICINE).build());
        var nurse = users.save(User.builder().email("record@test.invalid").nickname("기록작성자").name("기록작성자")
                .provider(Provider.LOCAL).providerId("record-test").role(Role.NURSE).department(department).build());
        var patient = patients.save(Patient.builder().name("가상기록환자").chartNumber("RECORD-TEST").age(40).gender(Gender.F).department(department).isAdmitted(true).build());
        Long nurseId = nurse.getId(), patientId = patient.getId();
        var note = notes.createNursingNote(nurseId, NursingNoteRequest.builder().patientId(patientId).content("첫 기록").category(NoteCategory.OBSERVATION).build());
        var changed = notes.updateNursingNote(note.getId(), nurseId, NursingNoteRequest.builder().patientId(patientId).content("수정 기록").category(NoteCategory.OBSERVATION).version(note.getVersion()).build());
        assertThat(changed.getVersion()).isGreaterThan(note.getVersion());
        assertThatThrownBy(() -> notes.updateNursingNote(note.getId(), nurseId, NursingNoteRequest.builder().patientId(patientId).content("덮어쓰기").category(NoteCategory.OBSERVATION).version(note.getVersion()).build())).isInstanceOf(BusinessException.class);
        assertThat(notes.getPatientNursingNotes(patientId, nurseId).getFirst().getContent()).isEqualTo("수정 기록");
        var vital = vitals.createVitalSign(nurseId, VitalSignRequest.builder().patientId(patientId).heartRate(80).build());
        var newVital = vitals.updateVitalSign(vital.getId(), nurseId, VitalSignRequest.builder().patientId(patientId).heartRate(90).version(vital.getVersion()).build());
        assertThat(newVital.getVersion()).isGreaterThan(vital.getVersion());
        assertThatThrownBy(() -> vitals.updateVitalSign(vital.getId(), nurseId, VitalSignRequest.builder().patientId(patientId).heartRate(100).version(vital.getVersion()).build())).isInstanceOf(BusinessException.class);
        var output = io.createIntakeOutput(nurseId, IntakeOutputRequest.builder().patientId(patientId).outputUrine(0).build());
        var newOutput = io.updateIntakeOutput(output.getId(), nurseId, IntakeOutputRequest.builder().patientId(patientId).outputUrine(100).version(output.getVersion()).build());
        assertThat(newOutput.getVersion()).isGreaterThan(output.getVersion());
        assertThatThrownBy(() -> io.updateIntakeOutput(output.getId(), nurseId, IntakeOutputRequest.builder().patientId(patientId).outputUrine(200).version(output.getVersion()).build())).isInstanceOf(BusinessException.class);
    }
}
