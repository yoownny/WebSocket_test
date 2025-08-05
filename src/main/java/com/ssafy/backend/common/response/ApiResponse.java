package com.ssafy.backend.common.response;

import com.ssafy.backend.exception.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 표준화된 API 응답을 생성하는 유틸리티 클래스
 * ResponseWrapper를 사용하여 일관된 응답 형식 제공
 */
public class ApiResponse {
    // ===== 성공 응답들 =====
    // 상태코드 + 메시지
    public static ResponseEntity<SuccessResponse<Void>> success(HttpStatus status, String message) {
        SuccessResponse<Void> response = new SuccessResponse<>(
                status.value(),
                message,
                null
        );
        return ResponseEntity.status(status).body(response);
    }

    // 상태코드 + 헤더 + 메시지
    public static ResponseEntity<SuccessResponse<Void>> success(HttpStatus status, HttpHeaders headers, String message) {
        SuccessResponse<Void> response = new SuccessResponse<>(
                status.value(),
                message,
                null
        );
        return ResponseEntity.status(status).headers(headers).body(response);
    }

    // 상태코드 + 메시지 + 데이터
    public static <T> ResponseEntity<SuccessResponse<T>> success(HttpStatus status, String message, T data) {
        SuccessResponse<T> response = new SuccessResponse<>(
                status.value(),
                message,
                data
        );
        return ResponseEntity.status(status).body(response);
    }

    // 상태코드 + 헤더 + 메시지 + 데이터
    public static <T> ResponseEntity<SuccessResponse<T>> success(HttpStatus status, HttpHeaders headers, String message, T data) {
        SuccessResponse<T> response = new SuccessResponse<>(
                status.value(),
                message,
                data
        );
        return ResponseEntity.status(status).headers(headers).body(response);
    }

    // ===== 에러 응답들 =====
    public static ResponseEntity<ErrorResponse> error(ErrorCode errorCode) {
        ErrorResponse response = new ErrorResponse(
                errorCode.getStatus().value(),
                errorCode.name(),
                errorCode.getMessage()
        );
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * error(ErrorCode, String) - 커스텀 메시지
     */
    public static ResponseEntity<ErrorResponse> error(ErrorCode errorCode, String customMessage) {
        ErrorResponse response = new ErrorResponse(
                errorCode.getStatus().value(),
                errorCode.name(),
                customMessage
        );
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
}