package com.mediflow.emr.entity;

public enum Department {
    EMERGENCY("응급실"),
    INTERNAL_MEDICINE("내과"),
    SURGERY("외과"),
    OBSTETRICS_GYNECOLOGY("산부인과"),
    OTOLARYNGOLOGY("이비인후과"),
    PEDIATRICS("소아청소년과"),
    ORTHOPEDICS("정형외과"),
    NEUROLOGY("신경과"),
    PSYCHIATRY("정신건강의학과"),
    DERMATOLOGY("피부과"),
    OPHTHALMOLOGY("안과"),
    UROLOGY("비뇨의학과"),
    RADIOLOGY("영상의학과"),
    ANESTHESIOLOGY("마취통증의학과"),
    REHABILITATION("재활의학과"),
    FAMILY_MEDICINE("가정의학과"),
    NONE("부서 없음");

    private final String koreanName;

    Department(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getKoreanName() {
        return koreanName;
    }
}

