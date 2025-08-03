package com.ssafy.backend.config.jwt;

import com.ssafy.backend.config.security.CustomUserDetails;
import com.ssafy.backend.entity.User;
import com.ssafy.backend.exception.ErrorCode;
import com.ssafy.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * JWT 토큰을 검증하고 인증 정보를 설정하는 필터
 * JWT 관련 에러는 여기서 직접 처리 (ControllerExceptionAdvice와 분리)
 */
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    /**
     * 매 HTTP 요청마다 실행되는 JWT 검증 로직
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1단계: 인증이 필요없는 경로는 토큰 검증 건너뛰기
        if (isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2단계: Authorization 헤더에서 JWT 토큰 추출
        String accessToken = extractTokenFromHeader(request);
        if (accessToken == null) {
            // 토큰이 없는 경우 다음 필터로 넘어감 (Spring Security가 401 처리)
            filterChain.doFilter(request, response);
            return;
        }

        // 3단계: JWT 토큰 유효성 검증
        try {
            if (jwtUtil.isExpired(accessToken)) {
                sendErrorResponse(response, ErrorCode.TOKEN_EXPIRED);
                return;
            }

            // 4단계: 토큰 타입 확인 (access 토큰인지 확인)
            if (!"access".equals(jwtUtil.getCategory(accessToken))) {
                sendErrorResponse(response, ErrorCode.INVALID_TOKEN, "유효하지 않은 토큰 타입입니다.");
                return;
            }

            // 5단계: 토큰에서 사용자 ID 추출하여 DB에서 사용자 조회
            Long userId = jwtUtil.getUserId(accessToken);
            Optional<User> userOptional = userRepository.findById(userId);

            if (userOptional.isEmpty()) {
                sendErrorResponse(response, ErrorCode.USER_NOT_FOUND);
                return;
            }

            User user = userOptional.get();

            // 6단계: 삭제된 사용자 확인
            if (user.getDeleted()) {
                sendErrorResponse(response, ErrorCode.INVALID_USER, "삭제된 사용자입니다.");
                return;
            }

            // 7단계: 인증 정보를 Spring Security Context에 설정
            CustomUserDetails customUserDetails = new CustomUserDetails(user);
            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    customUserDetails,
                    null,
                    customUserDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // 8단계: 다음 필터로 요청 전달 (인증 완료)
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // JWT 파싱 실패 등의 예외 처리
            sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * 인증이 필요없는 공개 경로인지 확인
     */
    private boolean isPublicPath(String requestURI) {
        Set<String> publicPaths = Set.of(
                "/api/auth/login",
                "/api/auth/signup",
                "/api/auth/check-name",
                "/api/auth/refresh", // TODO: 추후 refresh 토큰 API 작업 필요
                // WebSocket 관련 경로들 추가
                "/ws/**",
                "/app/**",
                "/topic/**",
                "/queue/**",
                "/user/**"
        );
        return publicPaths.contains(requestURI);
    }

    /**
     * HTTP 헤더에서 JWT 토큰 추출
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7); // "Bearer " 제거
        }

        return null;
    }

    /**
     * JWT 관련 에러 응답 전송 (항상 401 Unauthorized)
     * 통일된 에러 구조: { statusCode, errorCode, message }
     */
    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        sendErrorResponse(response, errorCode, errorCode.getMessage());
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode, String customMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(
                String.format("{\"statusCode\":401,\"errorCode\":\"%s\",\"message\":\"%s\"}",
                        errorCode.name(), customMessage)
        );
    }
}