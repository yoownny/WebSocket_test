package com.ssafy.backend.game.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameStartResponseDto {
    private boolean success;
    private String errorMessage;
    private GameInfoResponseDto gameInfo;

    public static GameStartResponseDto success(GameInfoResponseDto gameInfo) {
        return GameStartResponseDto.builder()
                .success(true)
                .gameInfo(gameInfo)
                .build();
    }

    public static GameStartResponseDto failure(String errorMessage) {
        return GameStartResponseDto.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}