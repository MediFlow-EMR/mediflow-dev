package com.mediflow.emr.service;

import com.mediflow.emr.entity.DepartmentEntity;
import com.mediflow.emr.entity.Patient;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.entity.enums.Gender;
import com.mediflow.emr.entity.enums.Provider;
import com.mediflow.emr.entity.enums.Role;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.PatientRepository;
import com.mediflow.emr.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatientAccessServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final ClinicalAuditService clinicalAuditService = mock(ClinicalAuditService.class);
    private final PatientAccessService service = new PatientAccessService(
            userRepository, patientRepository, clinicalAuditService);

    private DepartmentEntity wardA;
    private DepartmentEntity wardB;
    private Patient patientInWardA;

    @BeforeEach
    void setUp() {
        wardA = DepartmentEntity.builder().id(1L).name("내과병동").code("MW").build();
        wardB = DepartmentEntity.builder().id(2L).name("외과병동").code("SW").build();
        patientInWardA = Patient.builder()
                .id(100L)
                .chartNumber("C000100")
                .name("가상환자")
                .age(50)
                .gender(Gender.F)
                .department(wardA)
                .isAdmitted(true)
                .build();
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patientInWardA));
    }

    @Test
    void sameDepartmentNurseCanAccessPatient() {
        User nurse = user(10L, Role.NURSE, wardA, true, false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(nurse));

        Patient result = service.requireAccessiblePatient(10L, 100L);

        assertThat(result).isSameAs(patientInWardA);
    }

    @Test
    void otherDepartmentNurseIsDenied() {
        User nurse = user(11L, Role.NURSE, wardB, true, false);
        when(userRepository.findById(11L)).thenReturn(Optional.of(nurse));

        assertThatThrownBy(() -> service.requireAccessiblePatient(11L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void lockedAccountIsDeniedEvenInSameDepartment() {
        User nurse = user(12L, Role.NURSE, wardA, true, true);
        when(userRepository.findById(12L)).thenReturn(Optional.of(nurse));

        assertThatThrownBy(() -> service.requireAccessiblePatient(12L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void administratorCanAccessAcrossDepartments() {
        User admin = user(1L, Role.ADMIN, null, true, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThat(service.requireAccessiblePatient(1L, 100L)).isSameAs(patientInWardA);
    }

    private User user(Long id, Role role, DepartmentEntity department, boolean active, boolean locked) {
        return User.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .provider(Provider.LOCAL)
                .providerId("user-" + id)
                .role(role)
                .department(department)
                .isActive(active)
                .isLocked(locked)
                .build();
    }
}
