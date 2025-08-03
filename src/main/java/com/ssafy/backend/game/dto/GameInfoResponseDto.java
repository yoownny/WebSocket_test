package com.ssafy.backend.game.dto;

import com.ssafy.backend.memory.Player;
import com.ssafy.backend.memory.type.RoomState;
import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameInfoResponseDto {
    @Builder.Default
    private String event = "game_started";
    private GameStartedData data;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class GameStartedData {
        private Long roomId;
        private RoomState roomState;
        private GameStatus gameStatus;
        private CurrentTurn currentTurn;
        private List<Player> players;
//        private Problem problem;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class GameStatus {
        private int remainingQuestions;
        private int totalQuestions;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CurrentTurn {
        private Long questionerId;
        private String nickname;
        private int turnIndex;
    }
}

