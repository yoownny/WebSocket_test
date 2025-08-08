package com.ssafy.backend.room.dto.response;

import com.ssafy.backend.memory.Player;
import com.ssafy.backend.memory.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferHostResponseResult {
    private Room room;
    private Player oldHost; // 기존 방장
    private Player newHost; // 새 방장
    private boolean accepted; // 수락 여부

    public static TransferHostResponseResult accepted(Room room, Player oldHost, Player newHost) {
        return TransferHostResponseResult.builder()
                .room(room)
                .oldHost(oldHost)
                .newHost(newHost)
                .accepted(true)
                .build();
    }

    public static TransferHostResponseResult declined(Room room, Player oldHost, Player newHost) {
        return TransferHostResponseResult.builder()
                .room(room)
                .oldHost(oldHost)
                .newHost(newHost)
                .accepted(false)
                .build();
    }
}
