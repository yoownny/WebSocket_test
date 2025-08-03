package com.ssafy.backend.config.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 클라이언트가 메시지를 보낼 때 사용할 endpoint 설정 (즉, /app)
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic","/queue"); // 서버 -> 클라 (브로드캐스트, 개별 메시지)
        config.setApplicationDestinationPrefixes("/app"); // 클라 -> 서버
    }

    // WebSocket 연결을 위한 엔드포인트 등록
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // 클라이언트 연결 엔드포인트
                .setAllowedOriginPatterns("*") // CORS 허용
                .withSockJS(); // SockJS fallback 지원
    }
}
