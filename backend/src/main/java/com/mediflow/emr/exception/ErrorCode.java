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
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal error");

    private final HttpStatus status;
    private final String defaultMessage;
}

