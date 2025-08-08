package com.ssafy.backend.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameInfoResultDto {
    private Integer TimeLimit;
    private GameInfoResponseDto gameInfoResponseDto;
}

