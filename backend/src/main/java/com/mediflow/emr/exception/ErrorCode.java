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
    VITAL_NOT_FOUND(HttpStatus.NOT_FOUND, "vital sign not found");

    private final HttpStatus status;
    private final String defaultMessage;
}
