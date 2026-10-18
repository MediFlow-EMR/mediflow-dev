package com.mediflow.emr.service;

import com.mediflow.emr.config.GeminiConfig;
import com.mediflow.emr.entity.*;
import com.mediflow.emr.entity.enums.Gender;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandoverGenerationTest {
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
    @Mock AssignmentRepository assignments;
    @Mock GeminiConfig config;
    @Mock ClinicalAuditService audit;
    @InjectMocks HandoverService service;

    @BeforeEach
    void assignedPatient() {
        Patient patient = Patient.builder().id(10L).name("가상환자").chartNumber("DEMO001")
                .age(60).gender(Gender.M).department(DepartmentEntity.builder().name("시연부서").build()).build();
        Shift shift = Shift.builder().id(99L).date(LocalDate.of(2026, 10, 9)).startTime(LocalTime.of(16, 0)).endTime(LocalTime.MIDNIGHT).build();
        when(shifts.findById(99L)).thenReturn(Optional.of(shift));
        when(assignments.findByNurseIdAndShiftId(1L, 99L))
                .thenReturn(List.of(Assignment.builder().patient(patient).build()));
    }

    @Test
    void missingRecordsDoNotCallAiOrInventStableStatus() {
        String result = service.generateAiSummary(1L, 99L);
        assertThat(result).contains("**[가상환자 (DEMO001, 60세/남)]**", "해당 근무조 기록 없음")
                .doesNotContain("안정적", "정상", "처치 완료");
        verifyNoInteractions(gemini);
    }

    @Test
    void patientRegenerationAlsoSkipsAiWithoutRecords() {
        assertThat(service.generatePatientAiSummary(1L, 99L, 10L)).contains("해당 근무조 기록 없음");
        verifyNoInteractions(gemini);
    }

    @Test
    void restoresVerifiedPatientHeadingWithoutDependingOnMarkdownStyle() {
        when(vitals.findByPatientIdAndMeasuredAtBetween(eq(10L), any(), any()))
                .thenReturn(List.of(VitalSign.builder().spo2(95).build()));
        when(gemini.generateContent(anyString())).thenReturn("### [환자-001 (60세/남)]\n- 주요 변화: SpO2 95% 기록.");
        assertThat(service.generateAiSummary(1L, 99L))
                .contains("**[가상환자 (DEMO001, 60세/남)]**\n- 주요 변화: SpO2 95% 기록.");
    }

    @Test
    void rejectsInventedPatientIdentity() {
        when(vitals.findByPatientIdAndMeasuredAtBetween(eq(10L), any(), any()))
                .thenReturn(List.of(VitalSign.builder().spo2(95).build()));
        when(gemini.generateContent(anyString())).thenReturn("### [임의환자 (60세/남)]\n- 주요 변화: 임의 내용.");
        assertThatThrownBy(() -> service.generateAiSummary(1L, 99L)).isInstanceOf(BusinessException.class);
    }
    @Test
    void selectedHistoricalShiftControlsAllRecordQueries() {
        service.generateAiSummary(1L, 99L);
        var start = LocalDateTime.of(2026,10,9,16,0);
        var last = LocalDateTime.of(2026,10,10,0,0).minusNanos(1);
        verify(vitals).findByPatientIdAndMeasuredAtBetween(10L,start,last);
        verify(medications).findByPatientIdAndAdministeredAtBetween(10L,start,last);
        verify(intakeOutputs).findByPatientIdAndRecordedAtBetween(10L,start,last);
    }

    @Test
    void patientRegenerationUsesHistoricalNightCrossingMidnight() {
        when(shifts.findById(99L)).thenReturn(Optional.of(Shift.builder().id(99L)
                .date(LocalDate.of(2026,10,9)).startTime(LocalTime.of(23,0)).endTime(LocalTime.of(7,0)).build()));
        service.generatePatientAiSummary(1L, 99L, 10L);
        verify(vitals).findByPatientIdAndMeasuredAtBetween(10L,LocalDateTime.of(2026,10,9,23,0),
                LocalDateTime.of(2026,10,10,7,0).minusNanos(1));
    }

    @Test
    void testResultsOutsideShiftOrMissingTimeAreExcluded() {
        when(testResults.findByPatientIdOrderByResultDateDesc(10L)).thenReturn(List.of(
                TestResult.builder().resultDate(LocalDateTime.of(2026,10,9,16,0)).testName("selected-start").resultValue("in-shift").build(),
                TestResult.builder().resultDate(LocalDateTime.of(2026,10,10,0,0)).testName("next-shift").resultValue("excluded-end").build(),
                TestResult.builder().resultDate(LocalDateTime.of(2026,10,9,15,59)).testName("before-shift").resultValue("excluded-before").build(),
                TestResult.builder().testName("missing-time").resultValue("excluded-null").build()));
        when(gemini.generateContent(anyString())).thenReturn("**[환자-001 (60세/남)]**\n- 주요 변화: 검사 확인.");
        service.generateAiSummary(1L,99L);
        verify(gemini).generateContent(argThat(prompt -> prompt.contains("in-shift") && !prompt.contains("excluded-")));
    }

    @Test
    void latestVitalIsSelectedEvenWhenRepositoryReturnsOldestFirst() {
        when(vitals.findByPatientIdAndMeasuredAtBetween(eq(10L), any(), any())).thenReturn(List.of(
                VitalSign.builder().spo2(91).measuredAt(LocalDateTime.of(2026,10,9,16,0)).build(),
                VitalSign.builder().spo2(98).measuredAt(LocalDateTime.of(2026,10,9,22,0)).build()));
        when(gemini.generateContent(anyString())).thenReturn("**[환자-001 (60세/남)]**\n- 주요 변화: SpO2 98% 기록.");
        service.generateAiSummary(1L,99L);
        verify(gemini).generateContent(argThat(prompt -> prompt.contains("SpO2 98%") && !prompt.contains("SpO2 91%")));
    }

    @ParameterizedTest
    @CsvSource({"200,500,400,+300,섭취 초과", "0,0,300,-300,배설 초과", "100,0,100,0,섭취·배설량 동일"})
    void fluidBalanceUsesSuppliedArithmeticAndNeutralDirectionForBothGenerationPaths(
            int oral, int iv, int urine, String balance, String direction) {
        when(intakeOutputs.findByPatientIdAndRecordedAtBetween(eq(10L), any(), any()))
                .thenReturn(List.of(IntakeOutput.builder().intakeOral(oral).intakeIv(iv).outputUrine(urine).build()));
        when(gemini.generateContent(anyString())).thenReturn("**[환자-001 (60세/남)]**\n- 주요 변화: 수분 기록 확인.");
        service.generateAiSummary(1L,99L);
        service.generatePatientAiSummary(1L,99L,10L);
        verify(gemini, times(2)).generateContent(argThat(prompt -> prompt.contains("차이(섭취-배설) " + balance + "mL (" + direction + ")")
                && prompt.contains("상태가 좋다는 의미로 해석하지 마세요")));
    }

}
