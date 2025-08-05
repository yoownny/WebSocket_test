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
public class JoinRoomResult {
    private Room room; // 업데이트된 방 정보
    private Player joinedPlayer; // 입장한 플레이어 정보
    private boolean isHost; // 입장한 사용자가 방장인지 여부

    public static JoinRoomResult success(Room room, Player joinedPlayer, boolean isHost) {
        return JoinRoomResult.builder()
                .room(room)
                .joinedPlayer(joinedPlayer)
                .isHost(isHost)
                .build();
    }
}
