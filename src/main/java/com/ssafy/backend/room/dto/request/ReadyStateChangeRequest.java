package com.ssafy.backend.room.dto.request;

import com.ssafy.backend.memory.type.ReadyState;
import lombok.Getter;

@Getter
public class ReadyStateChangeRequest {
    private Long roomId;
    ReadyState readyState; // "READY" 또는 "WAITING"
}
