package com.mediflow.emr.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 애플리케이션 전반에서 사용되는 오류 코드를 정의하는 열거형
 * 각 오류 코드는 HTTP 상태 코드와 기본 메시지를 포함
 */
@AllArgsConstructor
@Getter
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "user not found"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "invalid token"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "forbidden"),
    CONFLICT(HttpStatus.CONFLICT, "다른 화면에서 변경된 기록입니다. 최신 내용을 다시 열어주세요."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "bad request"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal error"),

    // 이메일 인증 관련 에러 코드
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "verification not found"),
    VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "verification code expired"),
    VERIFICATION_INVALID(HttpStatus.BAD_REQUEST, "verification code invalid"),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "email not verified"),

    // 부서 관련 에러 코드
    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "department not found"),

    // 환자 관련 에러 코드
    PATIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "patient not found"),

    // 바이탈 관련 에러 코드
    VITAL_NOT_FOUND(HttpStatus.NOT_FOUND, "vital sign not found"),

    // 섭취배설량 관련 에러 코드
    IO_NOT_FOUND(HttpStatus.NOT_FOUND, "intake/output record not found"),

    // 간호기록 관련 에러 코드
    NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "nursing note not found"),

    // 투약 관련 에러 코드
    MEDICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "medication record not found"),

    // 오더 관련 에러 코드
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "medical order not found"),

    // 의료 기록 보존 관련 에러 코드
    MEDICAL_RECORD_CANNOT_BE_DELETED(HttpStatus.FORBIDDEN, "의료법 제23조에 따라 의료 기록은 10년간 보존해야 하므로 삭제할 수 없습니다");

    private final HttpStatus status;
    private final String defaultMessage;
}
