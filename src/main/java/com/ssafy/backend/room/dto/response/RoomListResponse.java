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
    private Integer totalCount;
    private String appliedFilter;

    public static RoomListResponse of(List<Room> rooms, String appliedFilter) {
        List<RoomResponse> roomResponses = rooms.stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());

        return RoomListResponse.builder()
                .rooms(roomResponses)
                .totalCount(rooms.size())
                .appliedFilter(appliedFilter)
                .build();
    }
}
