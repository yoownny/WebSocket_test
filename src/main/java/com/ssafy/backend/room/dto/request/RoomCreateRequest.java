package com.ssafy.backend.room.dto.request;

import lombok.Getter;

@Getter
public class RoomCreateRequest {
    private String title; // 문제 제목이 title로 매핑
    private int maxPlayers;
    private int timeLimit;
}
