package com.ssafy.backend.exception.model;

import com.ssafy.backend.exception.ErrorCode;

public class ForbiddenException extends BaseException {

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }
}