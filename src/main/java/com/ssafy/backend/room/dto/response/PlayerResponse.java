package com.ssafy.backend.room.dto.response;

import com.ssafy.backend.memory.Player;
import com.ssafy.backend.memory.type.PlayerRole;
import com.ssafy.backend.memory.type.PlayerState;
import com.ssafy.backend.memory.type.ReadyState;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class PlayerResponse {
    private Long userId;
    private String nickname;
    private PlayerRole role;
    private PlayerState state;
    private ReadyState readyState;

    public static PlayerResponse from(Player player) {
        return PlayerResponse.builder()
                .userId(player.getUserId())
                .nickname(player.getNickname())
                .role(player.getRole())
                .state(player.getState())
                .readyState(player.getReadyState())
                .build();
    }
}
