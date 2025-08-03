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

}
