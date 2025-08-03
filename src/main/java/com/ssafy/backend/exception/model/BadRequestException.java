package com.ssafy.backend.exception.model;

import com.ssafy.backend.exception.ErrorCode;

public class BadRequestException extends BaseException{
    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }
}
