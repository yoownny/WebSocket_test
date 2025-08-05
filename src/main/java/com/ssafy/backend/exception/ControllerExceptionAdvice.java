package com.ssafy.backend.exception;

import com.ssafy.backend.common.response.ApiResponse;
import com.ssafy.backend.common.response.ErrorResponse;
import com.ssafy.backend.exception.model.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ControllerExceptionAdvice {
    /**
     * 커스텀 비즈니스 예외 처리
     */
    @ExceptionHandler(BaseException.class)
    protected ResponseEntity<ErrorResponse> handleBaseException(BaseException exception) {
        log.warn("[{}] {} - {}",
                exception.getErrorCode().name(),
                exception.getClass().getSimpleName(),
                exception.getMessage());

        return ApiResponse.error(exception.getErrorCode(), exception.getMessage());
    }

    // 이 밑으로 Spring Web 관련 예외들 처리
    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception) {

        log.warn("Missing required parameter: {}", exception.getParameterName());

        String message = "필수 파라미터가 누락되었습니다: " + exception.getParameterName();

        return ApiResponse.error(ErrorCode.MISSING_PARAMETER, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception) {

        log.warn("HTTP message not readable: {}", exception.getMessage());

        return ApiResponse.error(ErrorCode.INVALID_REQUEST_BODY);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception) {

        // 검증 실패한 필드들을 조합 -> 사용자 친화적 메시지 생성
        StringBuilder sb = new StringBuilder();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            sb.append(error.getField()).append(": ").append(error.getDefaultMessage()).append(" ");
        }

        String message = sb.toString().trim();
        log.warn("Validation failed: {}", message);

        return ApiResponse.error(ErrorCode.VALIDATION_FAILED, message);
    }

    /**
     * 위에서 처리되지 않은 모든 예외들을 포착
     * 예: NullPointerException, IllegalArgumentException,
     *     RuntimeException, 기타 예상치 못한 에러들
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {
        log.error("Unhandled exception occurred: {}", exception.getMessage(), exception);

        return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
