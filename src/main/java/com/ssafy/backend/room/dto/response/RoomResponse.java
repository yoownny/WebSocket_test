package com.ssafy.backend.room.dto.response;

import com.ssafy.backend.memory.Room;
import com.ssafy.backend.memory.type.RoomState;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class RoomResponse {
    private Long roomId;
    private String title;
    private int maxPlayers;
    private int currentPlayers;
    private int timeLimit;
    private RoomState state;
    private Long hostId;
    private List<PlayerResponse> players;

    public static RoomResponse from(Room room) {
        return RoomResponse.builder()
                .roomId(room.getRoomId())
                .title(room.getTitle())
                .maxPlayers(room.getMaxPlayers())
                .currentPlayers(room.getCurrentPlayerCount())
                .timeLimit(room.getTimeLimit())
                .state(room.getState())
                .hostId(room.getHostId())
                .players(room.getPlayers().values().stream()
                        .map(PlayerResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
