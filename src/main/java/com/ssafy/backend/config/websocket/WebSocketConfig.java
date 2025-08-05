package com.ssafy.backend.config.websocket;

import com.ssafy.backend.config.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JWTUtil jwtUtil;

    // 클라이언트가 메시지를 보낼 때 사용할 endpoint 설정 (즉, /app)
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic","/queue"); // 서버 -> 클라 (브로드캐스트, 개별 메시지)
        config.setApplicationDestinationPrefixes("/app"); // 클라 -> 서버
        config.setUserDestinationPrefix("/user");
    }

    // WebSocket 연결을 위한 엔드포인트 등록
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // 클라이언트 연결 엔드포인트
                .setAllowedOriginPatterns("*") // CORS 허용
                .withSockJS(); // SockJS fallback 지원
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    authenticateUser(accessor);
                }

                return message;
            }
        });
    }
    private void authenticateUser(StompHeaderAccessor accessor) {
        try {
            // JWT 토큰 추출 시도
            String token = extractJwtToken(accessor);

            if (token != null && jwtUtil.validateToken(token)) {
                // JWT에서 사용자 정보 추출
                authenticateWithJwt(accessor, token);
            } else {
                // JWT 실패 시 fallback으로 헤더에서 직접 추출
                authenticateWithHeaders(accessor);
            }
        } catch (Exception e) {
            log.warn("WebSocket 인증 중 오류 발생: {}", e.getMessage());
            // 인증 실패해도 연결은 허용 (헤더 fallback)
            authenticateWithHeaders(accessor);
        }
    }

    private String extractJwtToken(StompHeaderAccessor accessor) {
        // Authorization 헤더에서 JWT 추출
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // 직접 token 헤더에서 추출 (클라이언트가 다른 방식으로 보낼 경우)
        String directToken = accessor.getFirstNativeHeader("token");
        if (directToken != null) {
            return directToken;
        }
        return null;
    }

    private void authenticateWithJwt(StompHeaderAccessor accessor, String token) {
        try {
            // JWT 토큰 유효성 재검증
            if (!jwtUtil.validateToken(token)) {
                log.warn("유효하지 않은 JWT 토큰");
                return;
            }

            // access 토큰인지 확인
            String category = jwtUtil.getCategory(token);
            if (!"access".equals(category)) {
                log.warn("WebSocket 연결에 access 토큰이 아닌 토큰 사용: {}", category);
                return;
            }

            // JWT에서 사용자 정보 추출
            Long userId = jwtUtil.getUserId(token);
            String nickname = jwtUtil.getNickname(token);
            String role = jwtUtil.getRole(token);

            // Principal 설정 및 세션에 정보 저장
            UserPrincipal principal = new UserPrincipal(userId, nickname, role);
            accessor.setUser(principal);

            // 세션 속성에도 저장 (기존 코드 호환성)
            accessor.getSessionAttributes().put("userId", userId);
            accessor.getSessionAttributes().put("nickname", nickname);
            accessor.getSessionAttributes().put("role", role);

            log.info("JWT 기반 WebSocket 인증 성공 - userId: {}, nickname: {}", userId, nickname);

        } catch (Exception e) {
            log.error("JWT 파싱 중 오류: {}", e.getMessage());
        }
    }

    private void authenticateWithHeaders(StompHeaderAccessor accessor) {
        // Fallback: 헤더에서 직접 사용자 정보 추출
        String userIdStr = accessor.getFirstNativeHeader("userId");
        String nickname = accessor.getFirstNativeHeader("nickname");

        if (userIdStr != null) {
            try {
                Long userId = Long.valueOf(userIdStr);

                // 기본 Principal 생성
                UserPrincipal principal = new UserPrincipal(userId, nickname, "USER");
                accessor.setUser(principal);

                // 세션 속성에 저장
                accessor.getSessionAttributes().put("userId", userId);
                if (nickname != null) {
                    accessor.getSessionAttributes().put("nickname", nickname);
                }

                log.info("헤더 기반 WebSocket 인증 성공 - userId: {}, nickname: {}", userId, nickname);

            } catch (NumberFormatException e) {
                log.warn("잘못된 userId 형식: {}", userIdStr);
            }
        } else {
            log.warn("WebSocket 인증 정보 없음 - JWT도 헤더도 없음");
        }
    }
}
