package com.ssafy.backend.memory;

import com.ssafy.backend.memory.type.RoomState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
@RequiredArgsConstructor
public class Room {
    private final Long roomId;
    private int maxPlayers;
    private int timeLimit;
    private RoomState state;
    private Long hostId;
    private Problem selectedProblem; // null이면 문제가 선택되지 않은 상태
    private Game currentGame; // 게임이 종료되면 null이 됨
    private Long targetUserId; // 방장 권한 요청을 받은 대상자 ID (null이면 요청 없음)

    // 생성자에서 maxPlayers, timeLimit 설정
    public Room(Long roomId, int maxPlayers, int timeLimit) {
        this.roomId = roomId;
        this.maxPlayers = maxPlayers;
        this.timeLimit = timeLimit;
    }

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

    // 방 설정 변경 가능 여부 확인 - 대기 중일 때만 변경 가능
    public boolean canUpdateSettings() {
        return state == RoomState.WAITING;
    }

    // 방 설정 변경 메서드들
    public void updateMaxPlayers(int maxPlayers) {
        if (!canUpdateSettings()) {
            throw new RuntimeException("게임이 시작된 방의 설정은 변경할 수 없습니다.");
        }
        if (maxPlayers < 2 || maxPlayers > 6) {
            throw new RuntimeException("인원수는 2명 이상 6명 이하로 설정해야 합니다.");
        }
        if (maxPlayers < getCurrentPlayerCount()) {
            throw new RuntimeException("현재 참가 인원보다 적은 최대 인원수로 설정할 수 없습니다.");
        }
        this.maxPlayers = maxPlayers;
    }

    public void updateTimeLimit(int timeLimit) {
        if (!canUpdateSettings()) {
            throw new RuntimeException("게임이 시작된 방의 설정은 변경할 수 없습니다.");
        }
        if (timeLimit < 10 || timeLimit > 15) {
            throw new RuntimeException("제한 시간은 10분 이상 15분 이하로 설정해야 합니다.");
        }
        this.timeLimit = timeLimit;
    }

    // 권한 넘기기 요청 관련 메소드들
    public boolean hasPendingHostTransfer() {
        return targetUserId != null;
    }

    public void setPendingHostTransfer(Long targetId) {
        this.targetUserId = targetId;
    }

    public void clearPendingHostTransfer() {
        this.targetUserId = null;
    }

    public boolean isValidHostTransferTarget(Long userId) {
        return targetUserId != null && targetUserId.equals(userId);
    }
}