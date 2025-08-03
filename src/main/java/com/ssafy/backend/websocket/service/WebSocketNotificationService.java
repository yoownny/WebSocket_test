package com.ssafy.backend.websocket.service;

import com.ssafy.backend.common.response.WebSocketResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
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
//        messagingTemplate.convertAndSendToUser(
//                String.valueOf(userId),
//                destination,
//                buildResponse(eventType, payload)
//        );
        try {
            log.info("📤 개인 메시지 전송 시도 - 사용자: {}, 목적지: {}, 이벤트: {}",
                    userId, destination, eventType);
            log.debug("📄 페이로드 타입: {}, 내용: {}",
                    payload != null ? payload.getClass().getSimpleName() : "null", payload);

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    destination,
                    buildResponse(eventType, payload)
            );

            log.info("✅ 개인 메시지 전송 완료 - 사용자: {}, 이벤트: {}", userId, eventType);

        } catch (Exception e) {
            log.error("❌ 개인 메시지 전송 실패 - 사용자: {}, 목적지: {}, 이벤트: {}, 오류: {}",
                    userId, destination, eventType, e.getMessage(), e);
        }
    }

    // 해당 destination을 구독한 모든 클라이언트에게 메시지 전송 (/topic)
    public void sendToTopic(String destination, String eventType, Object payload) {
//        messagingTemplate.convertAndSend(
//                destination,
//                buildResponse(eventType, payload)
//        );
        try {
            log.info("📢 토픽 메시지 전송 시도 - 목적지: {}, 이벤트: {}", destination, eventType);
            log.debug("📄 페이로드 타입: {}, 내용: {}",
                    payload != null ? payload.getClass().getSimpleName() : "null", payload);

            messagingTemplate.convertAndSend(
                    destination,
                    buildResponse(eventType, payload)
            );

            log.info("✅ 토픽 메시지 전송 완료 - 목적지: {}, 이벤트: {}", destination, eventType);

        } catch (Exception e) {
            log.error("❌ 토픽 메시지 전송 실패 - 목적지: {}, 이벤트: {}, 오류: {}",
                    destination, eventType, e.getMessage(), e);
        }
    }
}
