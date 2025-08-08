package com.ssafy.backend.room.dto.response;

import com.ssafy.backend.memory.Player;
import com.ssafy.backend.memory.Room;
import com.ssafy.backend.memory.type.ReadyState;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadyStateChangeResult {
    private Room room;
    private Long userId;
    private ReadyState newReadyState;
    private boolean allReady;
    private boolean canStartGame;

    // 개인 알림용 데이터
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PersonalStatusChange {
        private boolean success;
        private Long userId;
        private ReadyState newState;
        private String message;
    }

    // 브로드캐스트용 데이터
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoomReadyStateUpdate {
        private Long roomId;
        private List<ParticipantInfo> participants;
        private int currentCount;
        private int readyCount;
        private boolean allReady;
        private boolean canStartGame;

        @Getter
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class ParticipantInfo {
            private Long userId;
            private String nickname;
            private String role;
            private ReadyState readyState; // "READY" 또는 "WAITING"
        }

        public static RoomReadyStateUpdate from(Room room, boolean allReady, boolean canStartGame) {
            List<ParticipantInfo> participants = room.getPlayers().values().stream()
                    .map(p -> ParticipantInfo.builder()
                            .userId(p.getUserId())
                            .nickname(p.getNickname())
                            .role(p.getRole().name())
                            .readyState(p.getReadyState())
                            .build())
                    .collect(Collectors.toList());

            int readyCount = (int) room.getPlayers().values().stream()
                    .filter(Player::isReady)
                    .count();

            return RoomReadyStateUpdate.builder()
                    .roomId(room.getRoomId())
                    .participants(participants)
                    .currentCount(room.getCurrentPlayerCount())
                    .readyCount(readyCount)
                    .allReady(allReady)
                    .canStartGame(canStartGame)
                    .build();
        }
    }

    public static ReadyStateChangeResult success(Room room, Long userId, ReadyState newReadbyState, boolean allReady, boolean canStartGame) {
        return ReadyStateChangeResult.builder()
                .room(room)
                .userId(userId)
                .newReadyState(newReadbyState)
                .allReady(allReady)
                .canStartGame(canStartGame)
                .build();
    }
}
