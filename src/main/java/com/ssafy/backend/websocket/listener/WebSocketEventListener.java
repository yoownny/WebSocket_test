package com.ssafy.backend.websocket.listener;

import com.ssafy.backend.config.jwt.JWTUtil;
import com.ssafy.backend.memory.Room;
import com.ssafy.backend.memory.repository.RoomRepository;
import com.ssafy.backend.room.dto.response.RoomResponse;
import com.ssafy.backend.room.service.RoomService;
import com.ssafy.backend.websocket.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {
    private final RoomRepository roomRepository;
    private final RoomService roomService;
    private final WebSocketNotificationService webSocketNotificationService;
    private final JWTUtil jwtUtil;

    // 연결시 헤더에 userId를 넣는 방식
//    @EventListener
//    public void handleSessionConnected(SessionConnectEvent event) {
//        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
//        String userId = accessor.getFirstNativeHeader("userId");
//        if (userId != null) {
//            accessor.getSessionAttributes().put("userId", Long.valueOf(userId));
//            log.info("✅ WebSocket 연결됨 - userId: {}", userId);
//        } else {
//            log.info("❌ userId가 없음");
//        }
//    }

    // 연결시 헤더에 access 토큰을 넣는 방식
    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        // Authorization 헤더에서 JWT 토큰 가져오기
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7); // "Bearer " 제거

                // JWT 토큰 검증 및 userId 추출
                if (!jwtUtil.isExpired(token) && "access".equals(jwtUtil.getCategory(token))) {
                    Long userId = jwtUtil.getUserId(token);
                    String nickname = jwtUtil.getNickname(token);

                    // 세션에 사용자 정보 저장
                    accessor.getSessionAttributes().put("userId", userId);
                    accessor.getSessionAttributes().put("nickname", nickname);

                    log.info("WebSocket 연결됨 - userId: {}, nickname: {}", userId, nickname);
                } else {
                    log.warn("유효하지 않은 토큰");
                }
            } catch (Exception e) {
                log.error("JWT 토큰 파싱 실패: {}", e.getMessage());
            }
        } else {
            log.info("Authorization 헤더가 없음");
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = (Long) accessor.getSessionAttributes().get("userId");

        if (userId != null) {
            Long currentRoomId = roomRepository.getCurrentRoom(userId);
            try {
                Room updatedRoom = roomService.leaveRoom(currentRoomId, userId);
                webSocketNotificationService.sendToTopic("/topic/room/" + currentRoomId, "PLAYER_DISCONNECTED", userId);

                if (updatedRoom != null) {
                    webSocketNotificationService.sendToTopic("/topic/room/" + currentRoomId, "HOST_CHANGED",
                            RoomResponse.from(updatedRoom));
                }
            } catch (Exception e) {
                log.error("연결 해제 시 방 퇴장 처리 실패: userId={}, roomId={}", userId, currentRoomId, e);
            }
        }
    }
}


