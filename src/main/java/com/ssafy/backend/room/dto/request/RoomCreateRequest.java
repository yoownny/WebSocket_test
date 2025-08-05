package com.ssafy.backend.room.dto.request;

import lombok.Getter;

@Getter
public class RoomCreateRequest {
    private int maxPlayers;
    private int timeLimit;
}
