package com.ssafy.backend.websocket.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
@Slf4j
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    // WebSocket 핸드셰이크 단계에서 사용자 인증 정보를 결정할 때 호출되는 메서드
    // 리턴 Principal -> WebSocket 세션에 연결되어 @MessageMapping, ChannelInterceptor, EventListener 등에서
    //accessor.getUser()로 접근 가능
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String userId = ((ServletServerHttpRequest) request)
                .getServletRequest()
                .getHeader("userId");

        // WebSocket 핸드셰이크 자체가 오는지
        log.info("🌐 WebSocket Handshake - IP: {}, userId: {}",
                request.getRemoteAddress(), userId);
        if (userId == null) return null;

        return () -> userId; // Principal.getName() == userId
    }
}

