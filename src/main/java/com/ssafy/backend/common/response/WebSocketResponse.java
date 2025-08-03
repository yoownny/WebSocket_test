package com.ssafy.backend.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class WebSocketResponse<T> {
    private String eventType; // "ERROR", "GAME_STARTED"
    private T payload; // 실제 응답 데이터 (성공 시 데이터, 실패 시 응답 메시지)
}
