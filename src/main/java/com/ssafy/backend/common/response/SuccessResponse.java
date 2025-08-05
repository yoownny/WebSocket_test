package com.ssafy.backend.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모든 API 응답의 표준 형식을 정의하는 래퍼 클래스
 * 프론트엔드에서 일관된 방식으로 응답을 처리할 수 있도록 통일된 구조 제공
 *
 * @param <T> 실제 데이터의 타입
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SuccessResponse<T> {

    /** HTTP 상태 코드 (200, 201, 400, 401, 404 등) */
    private int statusCode;

    /** 사용자에게 보여줄 응답 메시지 */
    private String message;

    /** 실제 응답 데이터 (성공 시 데이터, 실패 시 보통 null) */
    private T data;
}