package com.ssafy.backend.exception.model;

import com.ssafy.backend.exception.ErrorCode;

public class NotFoundException extends BaseException {

    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}