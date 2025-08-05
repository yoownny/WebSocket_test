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
    private List<RoomResponse> rooms;      // 방 목록
    private Integer totalCount;            // 전체 방 개수
    private Integer currentPage;           // 현재 페이지
    private Integer totalPages;            // 전체 페이지 수
    private Boolean hasNext;               // 다음 페이지 존재 여부
    private Boolean hasPrevious;           // 이전 페이지 존재 여부

    public static RoomListResponse of(List<Room> rooms, int page, int size, int totalCount) {
        List<RoomResponse> roomResponses = rooms.stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) totalCount / size);

        return RoomListResponse.builder()
                .rooms(roomResponses)
                .totalCount(totalCount)
                .currentPage(page)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();
    }
}
