package com.ssafy.backend.memory;

import com.ssafy.backend.memory.type.PlayerRole;
import com.ssafy.backend.memory.type.RoomState;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import java.util.*;

@Setter
@Getter
@RequiredArgsConstructor
public class Room {
    private final Long roomId;
    private final int maxPlayers;
    private final int timeLimit;

    private String title;
    private RoomState state;
    private Long hostId;
    private Problem selectedProblem; // null이면 문제가 선택되지 않은 상태
    private Game currentGame; // 게임이 종료되면 null이 됨

    // 플레이어 입장 순서 기록
    private final List<Long> playerOrder = new ArrayList<>();
    // 현재 방에 있는 플레이어 정보
    private final Map<Long, Player> players = new HashMap<>();

    // 현재 방에 참가한 플레이어 수 조회
    public int getCurrentPlayerCount() {
        return players.size();
    }

    // 방이 비어있는지 확인
    public boolean isEmpty() {
        return players.isEmpty();
    }

    // 방이 가득 찼는지 확인
    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    // 특정 사용자가 이 방에 참가했는지 확인
    public boolean hasPlayer(Long userId) {
        return players.containsKey(userId);
    }

    // 특정 사용자의 정보 조회
    public Player getPlayer(Long userId) {
        return players.get(userId);
    }

    // 현재 방에 새로운 플레이어가 입장할 수 있는지 확인
    public boolean canJoin() {
        return state == RoomState.WAITING && !isFull();
    }
}