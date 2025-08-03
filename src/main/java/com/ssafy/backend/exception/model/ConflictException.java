package com.ssafy.backend.exception.model;

import com.ssafy.backend.exception.ErrorCode;

public class ConflictException extends BaseException {

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }
}
