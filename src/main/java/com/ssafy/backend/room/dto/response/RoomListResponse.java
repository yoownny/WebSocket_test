package com.ssafy.backend.room.dto.response;

import com.ssafy.backend.memory.Room;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class RoomListResponse {
    private List<RoomResponse> rooms;
    private int totalCount;

    public static RoomListResponse from(List<Room> rooms) {
        List<RoomResponse> roomResponses = rooms.stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());

        return RoomListResponse.builder()
                .rooms(roomResponses)
                .totalCount(roomResponses.size())
                .build();
    }
}
