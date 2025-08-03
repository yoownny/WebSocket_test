package com.ssafy.backend.websocket.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;

@Component
@Slf4j
public class WebSocketEventListener {

    // STOMP 프로토콜의 native header로 전달됨
    // SessionConnectEvent 발생 시 수동으로 꺼내서 sessionAttributes에 넣어줘야 이후 메시지 핸들러(@MessageMapping)에서도 접근 가능
    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getFirstNativeHeader("userId");
        String nickname = accessor.getFirstNativeHeader("nickname");

        if (userId != null) {
            accessor.getSessionAttributes().put("userId", Long.valueOf(userId));
            log.info("✅ WebSocket 연결됨 - userId: {}", userId);
            if (nickname != null) {
                accessor.getSessionAttributes().put("nickname", nickname);
                log.info("✅ nickname 저장: {}", nickname);
            } else {
                log.warn("❌ nickname이 없음");
            }
        } else {
            log.info("❌ userId가 없음");
        }
    }
}

