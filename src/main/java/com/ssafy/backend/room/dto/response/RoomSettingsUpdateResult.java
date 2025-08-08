package com.ssafy.backend.room.dto.response;

import com.ssafy.backend.memory.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSettingsUpdateResult {
    private Room room; // 업데이트된 방 정보
    private UpdatedSettings updatedSettings; // 변경된 설정 정보
    private Long changedBy; // 변경한 사용자 ID
    private String changedByNickname; // 변경한 사용자 닉네임
    private LocalDateTime changedAt; // 변경 시간

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdatedSettings {
        private Integer maxPlayers;
        private Integer timeLimit;

        public static UpdatedSettings of(Integer maxPlayers, Integer timeLimit) {
            return UpdatedSettings.builder()
                    .maxPlayers(maxPlayers)
                    .timeLimit(timeLimit)
                    .build();
        }
    }

    public static RoomSettingsUpdateResult success(Room room, Integer maxPlayers, Integer timeLimit, Long changedBy, String changedByNickname) {
        return RoomSettingsUpdateResult.builder()
                .room(room)
                .updatedSettings(UpdatedSettings.of(maxPlayers, timeLimit))
                .changedBy(changedBy)
                .changedByNickname(changedByNickname)
                .changedAt(LocalDateTime.now())
                .build();
    }
}
