package com.ssafy.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "닉네임 설정 (회원가입) 요청")
public class NicknameRequest {
    @Schema(description = "소셜 로그인 제공자로부터 받은 사용자 식별자", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjdkYzc4Y...")
    @NotBlank(message = "소셜 ID는 필수입니다.")
    private String socialId;

    @Schema(description = "사용자 닉네임", example = "바거슾마스터",
            minLength = 2, maxLength = 8)
    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    @Size(min = 2, max = 8, message = "닉네임은 2-8자 사이여야 합니다.")
    @Pattern(regexp = "^[ㄱ-ㅎ가-힣a-zA-Z0-9_-]{2,8}$",
            message = "닉네임은 한글, 영문, 숫자, 특수문자 '_', '-'만 사용 가능합니다.")
    private String nickname;
}