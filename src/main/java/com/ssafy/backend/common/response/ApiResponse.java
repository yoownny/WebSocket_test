package com.ssafy.backend.common.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 표준화된 API 응답을 생성하는 유틸리티 클래스
 * ResponseWrapper를 사용하여 일관된 응답 형식 제공
 */
public class ApiResponse {

    // 상태코드 + 메시지
    public static ResponseEntity<ResponseWrapper<Void>> success(HttpStatus status, String message) {
        ResponseWrapper<Void> wrapper = new ResponseWrapper<>(
                status.value(),
                message,
                null
        );
        return ResponseEntity.status(status).body(wrapper);
    }

    // 상태코드 + 헤더 + 메시지
    public static ResponseEntity<ResponseWrapper<Void>> success(HttpStatus status, HttpHeaders headers,String message) {
        ResponseWrapper<Void> wrapper = new ResponseWrapper<>(
                status.value(),
                message,
                null
        );
        return ResponseEntity.status(status).headers(headers).body(wrapper);
    }

    // 상태코드 + 메시지 + 데이터
    public static <T> ResponseEntity<ResponseWrapper<T>> success(HttpStatus status, String message, T data) {
        ResponseWrapper<T> wrapper = new ResponseWrapper<>(
                status.value(),
                message,
                data
        );
        return ResponseEntity.status(status).body(wrapper);
    }

    // 상태코드 + 헤더 + 메시지 + 데이터
    public static <T> ResponseEntity<ResponseWrapper<T>> success(HttpStatus status, HttpHeaders headers,String message, T data) {
        ResponseWrapper<T> wrapper = new ResponseWrapper<>(
                status.value(),
                message,
                data
        );
        return ResponseEntity.status(status).headers(headers).body(wrapper);
    }

    /**
     * error(ErrorCode) - 기본 메시지(간단 ver)
     */
    public static ResponseEntity<ResponseWrapper<Void>> error(com.ssafy.backend.exception.ErrorCode errorCode) {
        ResponseWrapper<Void> wrapper = new ResponseWrapper<>(
                errorCode.getStatus().value(),
                errorCode.getMessage(),
                null
        );
        return ResponseEntity.status(errorCode.getStatus()).body(wrapper);
    }

    /**
     * error(ErrorCode, String) - 커스텀 메시지
     */
    public static ResponseEntity<ResponseWrapper<Void>> error(com.ssafy.backend.exception.ErrorCode errorCode, String customMessage) {
        ResponseWrapper<Void> wrapper = new ResponseWrapper<>(
                errorCode.getStatus().value(),
                customMessage,
                null
        );
        return ResponseEntity.status(errorCode.getStatus()).body(wrapper);
    }
}