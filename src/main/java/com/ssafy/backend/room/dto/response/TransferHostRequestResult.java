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
public class TransferHostRequestResult {
    private Room room;
    private Player requester; // 권한 넘기기를 요청한 방장
    private Player target; // 권한을 받을 대상자

    public static TransferHostRequestResult success(Room room, Player requester, Player target) {
        return TransferHostRequestResult.builder()
                .room(room)
                .requester(requester)
                .target(target)
                .build();
    }
}
