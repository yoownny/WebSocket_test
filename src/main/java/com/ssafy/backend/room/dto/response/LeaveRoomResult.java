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
public class LeaveRoomResult {
    private Room room; // 업데이트된 방 정보 (null이면 방 삭제됨)
    private boolean hostChanged; // 방장이 변경되었는지 여부
    private Player newHost; // 새로운 방장 정보 (방장 변경시에만)
    private Long leavingUserId; // 나간 사용자 ID


    // 방이 삭제된 경우(마지막 사람이 나감)
    public static LeaveRoomResult roomDeleted(Long leavingUserId) {
        return LeaveRoomResult.builder()
                .room(null)
                .hostChanged(false)
                .newHost(null)
                .leavingUserId(leavingUserId)
                .build();
    }

    // 일반 참가자가 나간 경우 (방장 변경 없음)
    public static LeaveRoomResult participantLeft(Room room, Long leavingUserId) {
        return LeaveRoomResult.builder()
                .room(room)
                .hostChanged(false)
                .newHost(null)
                .leavingUserId(leavingUserId)
                .build();
    }

    // 방장이 나가서 새 방장으로 변경된 경우
    public static LeaveRoomResult hostChanged(Room room, Player newHost, Long leavingUserId) {
        return LeaveRoomResult.builder()
                .room(room)
                .hostChanged(true)
                .newHost(newHost)
                .leavingUserId(leavingUserId)
                .build();
    }

    // 방이 삭제되었는지 확인
    public boolean isRoomDeleted() {
        return room == null;
    }
}
