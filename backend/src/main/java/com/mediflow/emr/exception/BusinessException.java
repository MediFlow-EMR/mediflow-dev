package com.mediflow.emr.exception;

import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 예외를 나타내는 클래스
 * ErrorCode를 통해 구체적인 오류 정보를 제공
 */
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    // 생성자: ErrorCode만을 인자로 받음
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }
    // 생성자: ErrorCode와 사용자 정의 메시지를 인자로 받음
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }


}

