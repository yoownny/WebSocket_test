package com.ssafy.backend.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "에러 응답")
public class ErrorResponse {
    @Schema(description = "HTTP 상태 코드", example = "400")
    private int statusCode;

    @Schema(description = "에러 코드", example = "VALIDATION_FAILED")
    private String errorCode;

    @Schema(description = "에러 메시지", example = "입력값이 올바르지 않습니다.")
    private String message;
}