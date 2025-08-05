// LoginRequest

package com.ssafy.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "소셜 로그인 요청")
public class LoginRequest {
    @Schema(description = "소셜 로그인 제공자", example = "google", allowableValues = {"google"})
    @NotBlank(message = "소셜 로그인 제공자는 필수입니다.")
    private String provider;  // "google"

    @Schema(description = "소셜 로그인 제공자로부터 받은 사용자 식별자", example = "google_123456789")
    @NotBlank(message = "소셜 ID는 필수입니다.")
    private String socialId;
}