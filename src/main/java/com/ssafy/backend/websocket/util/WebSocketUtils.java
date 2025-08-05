package com.ssafy.backend.websocket.util;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WebSocketUtils {
    public static Long getUserIdFromSession(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("userId")) {
            return (Long) sessionAttributes.get("userId");
        }
        return 0L;
    }
    public static String getNicknameFromSession(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if(sessionAttributes != null && sessionAttributes.containsKey("nickname")) {
            return (String) sessionAttributes.get("nickname");
        }
        return "";
    }

//
//    /**
//     * WebSocket 세션에서 userId 추출
//     * @param headerAccessor SimpMessageHeaderAccessor 또는 StompHeaderAccessor
//     * @return userId (없으면 null)
//     */
//    public static Long getUserIdFromSession(SimpMessageHeaderAccessor headerAccessor) {
//        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
//        if (sessionAttributes != null && sessionAttributes.containsKey("userId")) {
//            return (Long) sessionAttributes.get("userId");
//        }
//        return null;
//    }
//
//    /**
//     * WebSocket 세션에서 userId 추출 (기본값 지원)
//     * @param headerAccessor SimpMessageHeaderAccessor 또는 StompHeaderAccessor
//     * @param defaultValue userId가 없을 때 반환할 기본값
//     * @return userId 또는 기본값
//     */
//    public static Long getUserIdFromSession(SimpMessageHeaderAccessor headerAccessor, Long defaultValue) {
//        Long userId = getUserIdFromSession(headerAccessor);
//        return userId != null ? userId : defaultValue;
//    }
//
//    /**
//     * 인증된 사용자인지 확인
//     * @param headerAccessor SimpMessageHeaderAccessor 또는 StompHeaderAccessor
//     * @return 인증 여부
//     */
//    public static boolean isAuthenticated(SimpMessageHeaderAccessor headerAccessor) {
//        return getUserIdFromSession(headerAccessor) != null;
//    }
}
