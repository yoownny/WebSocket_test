package com.ssafy.backend.config.security;

import com.ssafy.backend.config.jwt.JWTFilter;
import com.ssafy.backend.config.jwt.JWTUtil;
import com.ssafy.backend.exception.ErrorCode;
import com.ssafy.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration // 설정 파일
@EnableWebSecurity // spring security 기능 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTUtil jwtUtil;
    private final AuthenticationConfiguration authenticationConfiguration;


    /**
     * 역할 계층 구조 설정
     * ADMIN > USER 순서로 권한 상속
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("USER")     // ADMIN은 USER 권한 포함
                .build();
    }

    /**
     * AuthenticationManager Bean 등록
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Spring Security 메인 설정
     * OAuth + JWT 기반 인증 시스템 구성
     */
    @Bean //  HTTP 요청이 들어올 때마다 필터부터 실행
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository userRepository) throws Exception {
        http.csrf(csrf -> csrf.disable()) // csrf 공격 방지 비활성화
                .cors(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable()) // 아이디/비밀번호 팝업 창 비활성화
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 경로들
                        .requestMatchers(
                                "/api/auth/login",           // 로그인
                                "/api/auth/refresh",         // JWT 토큰 갱신
                                "/api/auth/nickname",          // 닉네임 설정(회원가입)
                                "/api/auth/check-nickname",      // 닉네임 중복 확인
                                "/api/auth/logout",              // 로그아웃
                                "/api/auth/nickname",        // 닉네임 설정(회원가입)
                                "/api/users/check-nickname", // 닉네임 중복 확인
                                "/api/users/{userId}",      // 사용자 정보 조회
                                "/api/rankings/**",
                                "/ws/**",                    // WebSocket 연결 엔드포인트
                                "/app/**",                   // STOMP 클라이언트 → 서버 메시지
                                "/topic/**",                 // STOMP 서버 → 클라이언트 브로드캐스트
                                "/queue/**",                  // STOMP 서버 → 클라이언트 개인 메시지
                                "swagger-ui/**",
                                "v3/api-docs/**"
                                ).permitAll()
                        // 나머지는 모두 인증 필요
                        .anyRequest().authenticated()
                )
                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                .addFilterBefore(new JWTFilter(jwtUtil, userRepository), UsernamePasswordAuthenticationFilter.class)
                // 세션 사용 안 함 (JWT는 Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 예외 처리 설정
                .exceptionHandling(ex -> ex
                        // 인증 실패 시 (401 Unauthorized)
                        .authenticationEntryPoint((request, response, authException) -> {
                            ErrorCode errorCode = ErrorCode.INVALID_USER;
                            response.setStatus(errorCode.getStatus().value());
                            response.setContentType("application/json; charset=UTF-8");
                            response.getWriter().write(
                                    String.format("{\"statusCode\":%d,\"errorCode\":\"%s\",\"message\":\"%s\"}",
                                            errorCode.getStatus().value(),
                                            errorCode.name(),
                                            errorCode.getMessage())
                            );
                        })
                        // 권한 부족 시 (403 Forbidden)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
                            response.setStatus(errorCode.getStatus().value());
                            response.setContentType("application/json; charset=UTF-8");
                            response.getWriter().write(
                                    String.format("{\"statusCode\":%d,\"errorCode\":\"%s\",\"message\":\"%s\"}",
                                            errorCode.getStatus().value(),
                                            errorCode.name(),
                                            errorCode.getMessage())
                            );
                        })
                );


        return http.build();
    }
}
