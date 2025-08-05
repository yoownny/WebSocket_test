package com.ssafy.backend.common.swagger;

/**
 * Swagger API 문서화를 위한 응답 예시 상수 클래스
 */
public class SwaggerConstants {

    // ===== 성공 응답 예시 =====
    public static final String LOGIN_SUCCESS_EXAMPLE = """
        {
          "statusCode": 200,
          "message": "로그인이 성공적으로 처리되었습니다.",
          "data": {
            "userId": 1,
            "socialId": "google_123456789",
            "provider": "GOOGLE",
            "nickname": "바거슾마스터",
            "role": "USER",
            "createdAt": "2024-01-15T10:30:00",
            "deleted": false
          }
        }
        """;

    public static final String SIGNUP_SUCCESS_EXAMPLE = """
        {
          "statusCode": 201,
          "message": "회원가입 및 로그인이 성공적으로 완료되었습니다.",
          "data": {
            "userId": 1,
            "socialId": "google_123456789",
            "provider": "GOOGLE",
            "nickname": "바거슾마스터",
            "role": "USER",
            "createdAt": "2024-01-15T10:30:00",
            "deleted": false
          }
        }
        """;

    public static final String NICKNAME_AVAILABLE_EXAMPLE = """
        {
          "statusCode": 200,
          "message": "사용 가능한 닉네임입니다.",
          "data": null
        }
        """;

    public static final String TOKEN_REFRESH_SUCCESS_EXAMPLE = """
        {
          "statusCode": 200,
          "message": "토큰이 성공적으로 갱신되었습니다.",
          "data": null
        }
        """;

    public static final String LOGOUT_SUCCESS_EXAMPLE = """
        {
          "statusCode": 200,
          "message": "로그아웃되었습니다.",
          "data": null
        }
        """;

    // ===== 에러 응답 예시 =====
    public static final String VALIDATION_FAILED_EXAMPLE = """
        {
          "statusCode": 400,
          "errorCode": "VALIDATION_FAILED",
          "message": "입력값이 올바르지 않습니다."
        }
        """;

    public static final String USER_NOT_FOUND_EXAMPLE = """
        {
          "statusCode": 401,
          "errorCode": "USER_NOT_FOUND",
          "message": "사용자를 찾을 수 없습니다."
        }
        """;

    public static final String NICKNAME_CONFLICT_EXAMPLE = """
        {
          "statusCode": 409,
          "errorCode": "NICKNAME_CONFLICT",
          "message": "이미 사용중인 닉네임입니다."
        }
        """;

    public static final String USER_CONFLICT_EXAMPLE = """
        {
          "statusCode": 409,
          "errorCode": "USER_CONFLICT",
          "message": "이미 존재하는 사용자입니다."
        }
        """;

    public static final String TOKEN_NOT_FOUND_EXAMPLE = """
        {
          "statusCode": 401,
          "errorCode": "TOKEN_NOT_FOUND",
          "message": "토큰이 존재하지 않습니다."
        }
        """;

    public static final String INVALID_TOKEN_EXAMPLE = """
        {
          "statusCode": 401,
          "errorCode": "INVALID_TOKEN",
          "message": "유효하지 않은 토큰입니다."
        }
        """;

    public static final String TOKEN_EXPIRED_EXAMPLE = """
        {
          "statusCode": 401,
          "errorCode": "TOKEN_EXPIRED",
          "message": "만료된 토큰입니다."
        }
        """;

    public static final String INTERNAL_SERVER_ERROR_EXAMPLE = """
        {
          "statusCode": 500,
          "errorCode": "INTERNAL_SERVER_ERROR",
          "message": "서버 내부 오류가 발생했습니다."
        }
        """;

    public static final String INVALID_USER_EXAMPLE = """
        {
          "statusCode": 401,
          "errorCode": "INVALID_USER",
          "message": "로그인이 필요합니다."
        }
        """;

    public static final String INVALID_NICKNAME_FORMAT_EXAMPLE = """
        {
          "statusCode": 400,
          "errorCode": "VALIDATION_FAILED",
          "message": "닉네임은 2-8자 사이여야 합니다."
        }
        """;

    public static final String MISSING_SOCIAL_ID_EXAMPLE = """
        {
          "statusCode": 400,
          "errorCode": "VALIDATION_FAILED",
          "message": "소셜 ID는 필수입니다."
        }
        """;
}