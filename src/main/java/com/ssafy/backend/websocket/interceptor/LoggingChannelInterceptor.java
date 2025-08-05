package com.ssafy.backend.websocket.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

// ChannelInterceptor - Spring Messaging의 인터페이스
// WebSocket 메시지 채널을 통과하는 메시지를 가로채서 전처리/후처리
@Slf4j
public class LoggingChannelInterceptor implements ChannelInterceptor {
    // 메시지가 클라이언트 → 서버 방향으로 들어오기 직전에 호출되는 메서드
    // todo;  (예: 로그인 여부 확인, 헤더 추출 등)
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // 일반 메시지를 STOMP 메시지로 감싸 STOMP 헤더에 접근하기 위함
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        // CONNECT, SUBSCRIBE, SEND, DISCONNECT
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        log.info("📨 [WebSocket] Command: {}, SessionId: {}, Headers: {}", command, accessor.getSessionId(), accessor.toNativeHeaderMap());

        if(command == StompCommand.CONNECT) {
            String userId = accessor.getFirstNativeHeader("userId");
            if (userId != null) {
                accessor.getSessionAttributes().put("userId", Long.valueOf(userId));
                log.info("✅ WebSocket 연결 성공 - userId: {}", userId);
            } else {
                log.warn("⚠️ CONNECT 요청에 userId가 없습니다.");
            }
        }else {
            Long userId = getUserIdFromSession(accessor);
            String destination = accessor.getDestination();
            log.info("📺 {} - userId: {}, destination: {}", command, userId, destination);
        }

        return message;
    }

    private Long getUserIdFromSession(StompHeaderAccessor accessor) {
        Object userId = accessor.getSessionAttributes().get("userId");
        return userId != null ? (Long) userId : null;
    }
}
