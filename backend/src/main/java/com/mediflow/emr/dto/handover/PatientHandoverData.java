package com.mediflow.emr.dto.handover;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 환자별 인수인계 데이터
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatientHandoverData {

    private Long patientId;
    private String patientName;
    private String roomNumber;
    private Integer age;
    private String gender;
    private String diagnosis;

    private List<String> nursingNotes = new ArrayList<>();
    private String latestVital;
    private List<String> testResults = new ArrayList<>();
    private List<String> medications = new ArrayList<>();
    private String intakeOutput;

    private Boolean isImportant = false;  // 중요 환자 여부
}

