package com.ssafy.backend.room.dto.request;

import lombok.Getter;

@Getter
public class RoomSettingsRequest {
    private Long roomId; // 설정을 변경할 방의 ID
    private Integer maxPlayers; // 변경할 최대 인원수
    private Integer timeLimit; // 변경할 제한 시간

}
