package com.ssafy.backend.common.swagger;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.ssafy.backend.common.response.ErrorResponse;
import com.ssafy.backend.common.response.SuccessResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증 관련 API의 공통 응답들을 정의하는 커스텀 어노테이션들
 */
public class AuthApiResponses {

    /**
     * 로그인 API 응답
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.LOGIN_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.VALIDATION_FAILED_EXAMPLE))),
            @ApiResponse(responseCode = "401", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.USER_NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.INTERNAL_SERVER_ERROR_EXAMPLE)))
    })
    public @interface LoginResponses {}

    /**
     * 닉네임 중복 확인 API 응답
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용 가능한 닉네임",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.NICKNAME_AVAILABLE_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "닉네임 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.INVALID_NICKNAME_FORMAT_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "닉네임 중복",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.NICKNAME_CONFLICT_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.INTERNAL_SERVER_ERROR_EXAMPLE)))
    })
    public @interface CheckNicknameResponses {}

    /**
     * 회원가입 API 응답
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.SIGNUP_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "invalid_nickname", value = SwaggerConstants.INVALID_NICKNAME_FORMAT_EXAMPLE),
                                    @ExampleObject(name = "missing_social_id", value = SwaggerConstants.MISSING_SOCIAL_ID_EXAMPLE)
                            })),
            @ApiResponse(responseCode = "409", description = "중복 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "nickname_conflict", value = SwaggerConstants.NICKNAME_CONFLICT_EXAMPLE),
                                    @ExampleObject(name = "user_conflict", value = SwaggerConstants.USER_CONFLICT_EXAMPLE)
                            })),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.INTERNAL_SERVER_ERROR_EXAMPLE)))
    })
    public @interface SignupResponses {}

    /**
     * 토큰 갱신 API 응답
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.TOKEN_REFRESH_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "401", description = "토큰 관련 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "token_not_found", value = SwaggerConstants.TOKEN_NOT_FOUND_EXAMPLE),
                                    @ExampleObject(name = "invalid_token", value = SwaggerConstants.INVALID_TOKEN_EXAMPLE),
                                    @ExampleObject(name = "token_expired", value = SwaggerConstants.TOKEN_EXPIRED_EXAMPLE)
                            })),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.USER_NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.INTERNAL_SERVER_ERROR_EXAMPLE)))
    })
    public @interface RefreshResponses {}

    /**
     * 로그아웃 API 응답
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.LOGOUT_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "invalid_user", value = SwaggerConstants.INVALID_USER_EXAMPLE),
                                    @ExampleObject(name = "token_expired", value = SwaggerConstants.TOKEN_EXPIRED_EXAMPLE)
                            })),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = SwaggerConstants.INTERNAL_SERVER_ERROR_EXAMPLE)))
    })
    public @interface LogoutResponses {}
}