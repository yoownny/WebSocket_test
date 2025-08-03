package com.ssafy.backend.exception.model;

import com.ssafy.backend.exception.ErrorCode;
import lombok.Getter;

/**
 * 모든 커스텀 예외의 기본 클래스
 * ErrorCode의 기본 메시지 또는 상황에 맞는 커스텀 메시지 사용 가능
 */
@Getter
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * ErrorCode와 함께 생성 (ErrorCode의 기본 메시지 사용)
     */
    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode와 커스텀 메시지로 생성 (상황에 맞는 구체적 메시지)
     */
    public BaseException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}