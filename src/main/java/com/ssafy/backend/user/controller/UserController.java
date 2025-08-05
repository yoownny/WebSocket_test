package com.ssafy.backend.user.controller;

import com.ssafy.backend.common.response.ApiResponse;
import com.ssafy.backend.common.response.SuccessResponse;
import com.ssafy.backend.user.dto.UserProfileDto;
import com.ssafy.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "사용자 관리", description = "사용자 공개 프로필 조회 등 사용자 관련 API")
public class UserController {

    private final UserService userService;

    /**
     * 사용자 공개 프로필 조회
     */
    @GetMapping("/{userId}")
    @Operation(
            summary = "사용자 공개 프로필 조회",
            description = "지정된 사용자의 공개 프로필 정보를 조회합니다. 로그인 없이도 접근 가능합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프로필 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = SuccessResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                  "statusCode": 200,
                                  "message": "사용자 프로필을 성공적으로 조회했습니다.",
                                  "data": {
                                    "userId": 1,
                                    "nickname": "바거슾마스터",
                                    "createdAt": "2024-01-15T10:30:00",
                                    "totalGames": 25,
                                    "wins": 18,
                                    "winRate": 72.0
                                  }
                                }
                                """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = com.ssafy.backend.common.response.ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                  "statusCode": 404,
                                  "errorCode": "USER_NOT_FOUND",
                                  "message": "사용자를 찾을 수 없습니다."
                                }
                                """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            schema = @Schema(implementation = com.ssafy.backend.common.response.ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                  "statusCode": 500,
                                  "errorCode": "INTERNAL_SERVER_ERROR",
                                  "message": "서버 내부 오류가 발생했습니다."
                                }
                                """)
                    )
            )
    })
    public ResponseEntity<SuccessResponse<UserProfileDto>> getUserProfile(
            @Parameter(description = "조회할 사용자 ID", required = true, example = "1")
            @PathVariable Long userId) {

        UserProfileDto userProfile = userService.getUserProfile(userId);

        return ApiResponse.success(
                HttpStatus.OK,
                "사용자 프로필을 성공적으로 조회했습니다.",
                userProfile
        );
    }
}