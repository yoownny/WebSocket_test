package com.ssafy.backend.room.dto.request;

import lombok.Getter;

@Getter
public class RoomProblemUpdateRequest {
    private String problemId; // CUSTOM: UUID, ORIGINAL: DB ID
    private String problemType; // "CUSTOM" 또는 "ORIGINAL"
}
