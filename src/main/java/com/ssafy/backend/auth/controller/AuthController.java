package com.ssafy.backend.auth.controller;

import com.ssafy.backend.auth.dto.LoginRequest;
import com.ssafy.backend.auth.dto.NicknameRequest;
import com.ssafy.backend.auth.service.AuthService;
import com.ssafy.backend.common.response.ApiResponse;
import com.ssafy.backend.common.response.SuccessResponse;
import com.ssafy.backend.common.swagger.AuthApiResponses;
import com.ssafy.backend.config.security.CustomUserDetails;
import com.ssafy.backend.entity.User;
import com.ssafy.backend.exception.ErrorCode;
import com.ssafy.backend.exception.model.BadRequestException;
import com.ssafy.backend.exception.model.NotFoundException;
import com.ssafy.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증 관리", description = "소셜 로그인, 회원가입, 토큰 관리 등 인증 관련 API")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    /**
     * 소셜 로그인
     */
    @PostMapping("/login")
    @Operation(
            summary = "소셜 로그인",
            description = "소셜 계정(구글)을 통해 로그인하고 JWT 토큰을 발급받습니다. 기존 사용자인 경우 즉시 로그인 처리됩니다."
    )
    @AuthApiResponses.LoginResponses
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {

        // 구글 로그인만 지원 확인
        if (!"google".equals(loginRequest.getProvider())) {
            return ApiResponse.error(ErrorCode.VALIDATION_FAILED);
        }

        try {
            // Spring Security 인증 처리
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getSocialId(), ""));
            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = customUserDetails.getUser();

            // 토큰 생성 및 쿠키 설정
            HttpHeaders headers = authService.generateAuthTokens(user, response);

            return ApiResponse.success(HttpStatus.OK, headers, "로그인이 성공적으로 처리되었습니다.", user);
        } catch (AuthenticationException e) {
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 닉네임 중복 확인
     */
    @GetMapping("/check-nickname")
    @Operation(
            summary = "닉네임 중복 확인",
            description = "회원가입 전 닉네임 중복 여부를 확인합니다."
    )
    @AuthApiResponses.CheckNicknameResponses
    public ResponseEntity<SuccessResponse<Void>> checkNickname(
            @Parameter(description = "확인할 닉네임 (2-8자, 한글/영문/숫자/특수문자(_,-) 허용)",
                    required = true, example = "바거슾마스터")
            @RequestParam String nickname) {
        authService.checkNickname(nickname);
        return ApiResponse.success(HttpStatus.OK, "사용 가능한 닉네임입니다.");
    }

    /**
     * 회원가입 + 자동 로그인
     */
    @PostMapping("/nickname")
    @Operation(
            summary = "닉네임 설정 (회원가입)",
            description = "신규 사용자 회원가입과 동시에 자동으로 로그인 처리하여 JWT 토큰을 발급합니다."
    )
    @AuthApiResponses.SignupResponses
    public ResponseEntity<SuccessResponse<User>> signup(
            @Valid @RequestBody NicknameRequest nicknameRequest,
            HttpServletResponse response) {

        User user = authService.join(nicknameRequest.getSocialId(), nicknameRequest.getNickname());
        HttpHeaders headers = authService.generateAuthTokens(user, response);

        return ApiResponse.success(HttpStatus.CREATED, headers, "회원가입 및 로그인이 성공적으로 완료되었습니다.", user);
    }

    /**
     * Access Token 재발급
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "토큰 갱신",
            description = "만료된 Access Token을 Refresh Token을 사용하여 재발급합니다."
    )
    @AuthApiResponses.RefreshResponses
    public ResponseEntity<SuccessResponse<Void>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        // 쿠키에서 refresh 토큰 추출
        String refreshToken = authService.extractRefreshTokenFromCookies(request);
        if (refreshToken == null) {
            throw new BadRequestException(ErrorCode.TOKEN_NOT_FOUND);
        }

        // 토큰 유효성 검증
        if (!authService.validateRefreshToken(refreshToken)) {
            throw new BadRequestException(ErrorCode.INVALID_TOKEN);
        }

        // 리프레시 토큰으로 사용자 ID 조회
        Long userId = authService.getUserIdByRefreshToken(refreshToken);
        if (userId == -1L) {
            throw new BadRequestException(ErrorCode.INVALID_TOKEN);
        }

        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        HttpHeaders headers = authService.generateAuthTokens(user, response);
        return ApiResponse.success(HttpStatus.OK, headers, "토큰이 성공적으로 갱신되었습니다.");
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = "현재 로그인된 사용자를 로그아웃하고 Refresh Token을 무효화합니다."
    )
    @AuthApiResponses.LogoutResponses
    public ResponseEntity<SuccessResponse<Void>> logout(
            HttpServletResponse response,
            Authentication authentication) {

        if (authentication != null && authentication.isAuthenticated()) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            // DB 에서 리프레시 토큰 삭제
            authService.logout(userDetails.getUserId());
        }

        // 쿠키 삭제
        Cookie refreshCookie = new Cookie("refresh", null);
        refreshCookie.setMaxAge(0); // 즉시 만료
        refreshCookie.setPath("/api/auth");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        response.addCookie(refreshCookie);

        return ApiResponse.success(HttpStatus.OK, "로그아웃되었습니다.");
    }
}