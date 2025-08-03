package com.ssafy.backend.websocket.service;

import com.ssafy.backend.common.response.WebSocketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {
    // STOMP 메시지 전송용
    private final SimpMessagingTemplate messagingTemplate;

    private <T> WebSocketResponse<T> buildResponse(String eventType, T payload) {
        return new WebSocketResponse<>(eventType, payload);
    }

    // 특정 사용자 한 명에게 메시지 전송 (/queue)
    public void sendToUser(Long userId, String destination, String eventType, Object payload) {
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                destination,
                buildResponse(eventType, payload)
        );
    }

    // 해당 destination을 구독한 모든 클라이언트에게 메시지 전송 (/topic)
    public void sendToTopic(String destination, String eventType, Object payload) {
        messagingTemplate.convertAndSend(
                destination,
                buildResponse(eventType, payload)
        );
    }
}
